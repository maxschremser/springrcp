/*
 * Copyright 2002-2004 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.springframework.richclient.application.support;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.binding.convert.ConversionService;
import org.springframework.binding.form.FormModel;
import org.springframework.binding.form.FormPropertyFaceDescriptorSource;
import org.springframework.binding.form.support.MessageSourceFormPropertyFaceDescriptorSource;
import org.springframework.binding.value.ValueChangeDetector;
import org.springframework.binding.value.support.DefaultValueChangeDetector;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.MessageSource;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.core.enums.LabeledEnumResolver;
import org.springframework.core.enums.StaticLabeledEnumResolver;
import org.springframework.richclient.application.ApplicationServices;
import org.springframework.richclient.application.DefaultConversionService;
import org.springframework.richclient.application.ViewDescriptorRegistry;
import org.springframework.richclient.application.config.ApplicationObjectConfigurer;
import org.springframework.richclient.application.config.DefaultApplicationObjectConfigurer;
import org.springframework.richclient.command.config.CommandConfigurer;
import org.springframework.richclient.command.config.DefaultCommandConfigurer;
import org.springframework.richclient.factory.ComponentFactory;
import org.springframework.richclient.factory.DefaultComponentFactory;
import org.springframework.richclient.form.binding.BinderSelectionStrategy;
import org.springframework.richclient.form.binding.BindingFactoryProvider;
import org.springframework.richclient.form.binding.swing.SwingBinderSelectionStrategy;
import org.springframework.richclient.form.binding.swing.SwingBindingFactoryProvider;
import org.springframework.richclient.form.builder.FormComponentInterceptor;
import org.springframework.richclient.form.builder.FormComponentInterceptorFactory;
import org.springframework.richclient.image.DefaultIconSource;
import org.springframework.richclient.image.DefaultImageSource;
import org.springframework.richclient.image.IconSource;
import org.springframework.richclient.image.ImageSource;
import org.springframework.richclient.security.ApplicationSecurityManager;
import org.springframework.richclient.security.SecurityControllerManager;
import org.springframework.richclient.security.support.DefaultApplicationSecurityManager;
import org.springframework.richclient.security.support.DefaultSecurityControllerManager;
import org.springframework.richclient.util.Assert;
import org.springframework.rules.RulesSource;
import org.springframework.rules.support.DefaultRulesSource;

/**
 * A default implementation of the ApplicationServices (service locator) interface. This
 * implementation uses information from the application context in order to locate
 * services using "magic" bean names. If requested beans are not defined in the context,
 * then a suitable default service will be created.
 * 
 * @author Larry Streepy
 */
public class DefaultApplicationServices implements ApplicationServices, ApplicationContextAware {

    private static final Log logger = LogFactory.getLog(DefaultApplicationServices.class);

    /** Map of services, keyed by service type (class). */
    private final Map services = Collections.synchronizedMap(new HashMap());

    /** Map of service types to default implementation builders. */
    private static final Map serviceImplBuilders = new HashMap();

    /** Application context with needed bean definitions. */
    private ApplicationContext applicationContext;

    /** ID of the ApplicationObjectConfigurer bean. */
    private String applicationObjectConfigurerBeanId;

    /**
     * Default Constructor.
     */
    public DefaultApplicationServices() {
    }

    /**
     * Constuct using the given application context.
     * 
     * @param applicationContext to use for locating named services (beans)
     */
    public DefaultApplicationServices( ApplicationContext applicationContext ) {
        setApplicationContext(applicationContext);
    }

    /**
     * Set the application context. We are ApplicationContextAware so this will happen
     * automatically if we are defined in the context. If not, then this method should be
     * called directly.
     */
    public void setApplicationContext( ApplicationContext applicationContext ) {
        this.applicationContext = applicationContext;
    }

    /**
     * @return application context
     */
    public ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    /**
     * Get a service of the indicated type. If no service definition for the requested
     * type is found in the application context, then a reasonable default implementation
     * will be created.
     * 
     * @param serviceType Type of service being requested
     * @return Service instance
     * @throws UnsupportedOperationException if the service is not found and no suitable
     *         default implementation is available.
     */
    public synchronized Object getService( Class serviceType ) {
        Assert.required(serviceType, "serviceType");
        Object service = services.get(serviceType);
        if( service == null ) {
            service = getDefaultImplementation(serviceType);
            if( service != null ) {
                services.put(serviceType, service);
            }
        }

        // If we still don't have an implementation, then it's a bust
        if( service == null ) {
            throw new UnsupportedOperationException("No service of type '" + serviceType + "' found.");
        }
        return service;
    }

    public boolean hasService( Class serviceType ) {
        Assert.required(serviceType, "serviceType");
        // TODO implement this
        throw new UnsupportedOperationException("Can not test service presence.");
    }

    /**
     * Add entries to the service registry. This is typically called from a bean
     * definition in the application context. The entryMap parameter must be a map with
     * keys that are either class instances (the serviceType) or the String name of the
     * class and values that are the implementation to use for that service.
     * 
     * @param entryMap Map of entries
     */
    public void setRegistryEntries( Map entryMap ) {
        Iterator iter = entryMap.entrySet().iterator();

        while( iter.hasNext() ) {
            Map.Entry entry = (Map.Entry) iter.next();
            Class serviceType = null;
            Object key = entry.getKey();

            // If the key is a String, convert it to a class
            if( key instanceof String ) {
                try {
                    serviceType = Class.forName((String) key);
                } catch( ClassNotFoundException e ) {
                    logger.error("Unable to convert key to Class", e);
                }
            } else if( key instanceof Class ) {
                serviceType = (Class) key;
            } else {
                logger.error("Invalid service entry key; must be String or Class, got: " + key.getClass());
            }

            // If we got something usable, then add the map entry
            if( serviceType != null ) {
                services.put(serviceType, entry.getValue());
            }
        }
    }

    /**
     * Set the Id of the context bean that holds the ApplicationObjectConfigurer instance
     * to use as the service. This mechanism is used to break a chicken/egg problem in
     * providing a reference to the AOC to the application services when the AOC needs to
     * ensure that the application services locator is constructed before the AOC.
     * 
     * @param aocBeanId Id of the ApplicationObjectConfigurer bean
     */
    public void setApplicationObjectConfigurerBeanId( String aocBeanId ) {
        applicationObjectConfigurerBeanId = aocBeanId;
    }

    /**
     * Set the application object configurer service implementation.
     * 
     * @param applicationObjectConfigurer
     */
    public void setApplicationObjectConfigurer( ApplicationObjectConfigurer applicationObjectConfigurer ) {
        services.put(ApplicationObjectConfigurer.class, applicationObjectConfigurer);
    }

    /**
     * Set the application security manager service implementation.
     * 
     * @param applicationSecurityManager instance to use
     */
    public void setApplicationSecurityManager( ApplicationSecurityManager applicationSecurityManager ) {
        services.put(ApplicationSecurityManager.class, applicationSecurityManager);
    }

    /**
     * Set the binder selection strategy service implementation
     * 
     * @param binderSelectionStrategy
     */
    public void setBinderSelectionStrategy( BinderSelectionStrategy binderSelectionStrategy ) {
        services.put(BinderSelectionStrategy.class, binderSelectionStrategy);
    }

    /**
     * Set the binding factory provider service implementation
     * 
     * @param bindingFactoryProvider
     */
    public void setBindingFactoryProvider( BindingFactoryProvider bindingFactoryProvider ) {
        services.put(BindingFactoryProvider.class, bindingFactoryProvider);
    }

    /**
     * Set the command configurer service implementation
     * 
     * @param commandConfigurer
     */
    public void setCommandConfigurer( CommandConfigurer commandConfigurer ) {
        services.put(CommandConfigurer.class, commandConfigurer);
    }

    /**
     * Set the component factory service implementation
     * 
     * @param componentFactory
     */
    public void setComponentFactory( ComponentFactory componentFactory ) {
        services.put(ComponentFactory.class, componentFactory);
    }

    /**
     * Set the conversion service service implementation
     * 
     * @param conversionService
     */
    public void setConversionService( ConversionService conversionService ) {
        services.put(ConversionService.class, conversionService);
    }

    /**
     * Set the form component interceptor factory service implementation
     * 
     * @param formComponentInterceptorFactory
     */
    public void setFormComponentInterceptorFactory( FormComponentInterceptorFactory formComponentInterceptorFactory ) {
        services.put(FormComponentInterceptorFactory.class, formComponentInterceptorFactory);
    }

    /**
     * Set the form property face descriptor source service implementation
     * 
     * @param formPropertyFaceDescriptorSource
     */
    public void setFormPropertyFaceDescriptorSource( FormPropertyFaceDescriptorSource formPropertyFaceDescriptorSource ) {
        services.put(FormPropertyFaceDescriptorSource.class, formPropertyFaceDescriptorSource);
    }

    /**
     * Set the icon source service implementation
     * 
     * @param iconSource
     */
    public void setIconSource( IconSource iconSource ) {
        services.put(IconSource.class, iconSource);
    }

    /**
     * Set the image source service implementation
     * 
     * @param imageSource
     */
    public void setImageSource( ImageSource imageSource ) {
        services.put(ImageSource.class, imageSource);
    }

    /**
     * Set the labeled enum resolver service implementation
     * 
     * @param labeledEnumResolver
     */
    public void setLabeledEnumResolver( LabeledEnumResolver labeledEnumResolver ) {
        services.put(LabeledEnumResolver.class, labeledEnumResolver);
    }

    /**
     * Set the message source service implementation
     * 
     * @param messageSource
     */
    public void setMessageSource( MessageSource messageSource ) {
        services.put(MessageSource.class, messageSource);
    }

    /**
     * Set the message source accessor service implementation
     * 
     * @param messageSourceAccessor
     */
    public void setMessageSourceAccesor( MessageSourceAccessor messageSourceAccessor ) {
        services.put(MessageSourceAccessor.class, messageSourceAccessor);
    }

    /**
     * Set the rules source service implementation
     * 
     * @param rulesSource
     */
    public void setRulesSource( RulesSource rulesSource ) {
        services.put(RulesSource.class, rulesSource);
    }

    /**
     * Set the security controller manager service implementation
     * 
     * @param securityControllerManager instance to use
     */
    public void setSecurityControllerManager( SecurityControllerManager securityControllerManager ) {
        services.put(SecurityControllerManager.class, securityControllerManager);
    }

    /**
     * Set the value change detector service imlpementation.
     * 
     * @param valueChangeDetector instance to use
     */
    public void setValueChangeDetector( ValueChangeDetector valueChangeDetector ) {
        services.put(ValueChangeDetector.class, valueChangeDetector);
    }

    /**
     * Set the view descriptor registry service implementation
     * 
     * @param viewDescriptorRegistry
     */
    public void setViewDescriptorRegistry( ViewDescriptorRegistry viewDescriptorRegistry ) {
        services.put(ViewDescriptorRegistry.class, viewDescriptorRegistry);
    }

    /**
     * Get the default implementation of a service according to the service type. If no
     * default implementation is available, then a null is returned.
     * 
     * @param serviceType Type of service requested
     * @return Default service implementation, or null if none defined
     */
    protected Object getDefaultImplementation( Class serviceType ) {
        Object impl = null;
        ImplBuilder builder = (ImplBuilder) serviceImplBuilders.get(serviceType);
        if( builder != null ) {
            impl = builder.build(this);
        }
        return impl;
    }

    /**
     * Internal interface used to provide default implementation builders.
     */
    protected interface ImplBuilder {
        /**
         * Build the service implementation.
         * 
         * @param applicationServices reference to service locator
         * @return service implementation
         */
        Object build( DefaultApplicationServices applicationServices );
    }

    protected static final ImplBuilder componentFactoryImplBuilder = new ImplBuilder() {
        public Object build( DefaultApplicationServices applicationServices ) {
            logger.info("Creating default service impl: ComponentFactory");
            return new DefaultComponentFactory();
        }
    };

    protected static final ImplBuilder formComponentInterceptorFactoryImplBuilder = new ImplBuilder() {
        public Object build( DefaultApplicationServices applicationServices ) {
            logger.info("Creating default service impl: FormComponentInterceptorFactory");
            return new FormComponentInterceptorFactory() {
                public FormComponentInterceptor getInterceptor( FormModel formModel ) {
                    return null;
                }
            };
        }
    };

    protected static final ImplBuilder applicationObjectConfigurerImplBuilder = new ImplBuilder() {
        public Object build( DefaultApplicationServices applicationServices ) {
            // First see if there is an AOC in the context, if not construct a default
            Object impl = null;
            String aocBeanId = applicationServices.applicationObjectConfigurerBeanId;
            if( aocBeanId != null ) {
                try {
                    impl = applicationServices.getApplicationContext().getBean(aocBeanId,
                            ApplicationObjectConfigurer.class);
                } catch( NoSuchBeanDefinitionException e ) {
                    logger.info("No object configurer found in context under name '" + aocBeanId
                            + "'; configuring defaults.");
                    impl = new DefaultApplicationObjectConfigurer();
                }
            } else {
                logger.info("No object configurer bean Id has been set; configuring defaults.");
                impl = new DefaultApplicationObjectConfigurer();
            }
            return impl;
        }
    };

    protected static final ImplBuilder commandConfigurerImplBuilder = new ImplBuilder() {
        public Object build( DefaultApplicationServices applicationServices ) {
            logger.info("Creating default service impl: CommandConfigurer");
            return new DefaultCommandConfigurer();
        }
    };

    protected static final ImplBuilder imageSourceImplBuilder = new ImplBuilder() {
        public Object build( DefaultApplicationServices applicationServices ) {
            logger.info("Creating default service impl: ImageSource");
            return new DefaultImageSource(new HashMap());
        }
    };

    protected static final ImplBuilder iconSourceImplBuilder = new ImplBuilder() {
        public Object build( DefaultApplicationServices applicationServices ) {
            logger.info("Creating default service impl: IconSource");
            return new DefaultIconSource();
        }
    };

    protected static final ImplBuilder rulesSourceImplBuilder = new ImplBuilder() {
        public Object build( DefaultApplicationServices applicationServices ) {
            logger.info("Creating default service impl: RulesSource");
            return new DefaultRulesSource();
        }
    };

    protected static final ImplBuilder conversionServiceImplBuilder = new ImplBuilder() {
        public Object build( DefaultApplicationServices applicationServices ) {
            logger.info("Creating default service impl: ConversionService");
            return new DefaultConversionService();
        }
    };

    protected static final ImplBuilder binderSelectionStrategyImplBuilder = new ImplBuilder() {
        public Object build( DefaultApplicationServices applicationServices ) {
            logger.info("Creating default service impl: BinderSelectionStrategy");
            return new SwingBinderSelectionStrategy();
        }
    };

    protected static final ImplBuilder formPropertyFaceDescriptorSourceImplBuilder = new ImplBuilder() {
        public Object build( DefaultApplicationServices applicationServices ) {
            logger.info("Creating default service impl: FormPropertyFaceDescriptorSource");
            return new MessageSourceFormPropertyFaceDescriptorSource();
        }
    };

    protected static final ImplBuilder bindingFactoryProviderImplBuilder = new ImplBuilder() {
        public Object build( DefaultApplicationServices applicationServices ) {
            logger.info("Creating default service impl: BindingFactoryProvider");
            return new SwingBindingFactoryProvider();
        }
    };

    protected static final ImplBuilder valueChangeDetectorImplBuilder = new ImplBuilder() {
        public Object build( DefaultApplicationServices applicationServices ) {
            logger.info("Creating default service impl: ValueChangeDetector");
            return new DefaultValueChangeDetector();
        }
    };

    protected static final ImplBuilder applicationSecurityManagerImplBuilder = new ImplBuilder() {
        public Object build( DefaultApplicationServices applicationServices ) {
            logger.info("Creating default service impl: ApplicationSecurityManager");
            return new DefaultApplicationSecurityManager(true);
        }
    };

    protected static final ImplBuilder SecurityControllerManagerImplBuilder = new ImplBuilder() {
        public Object build( DefaultApplicationServices applicationServices ) {
            logger.info("Creating default service impl: SecurityControllerManager");
            return new DefaultSecurityControllerManager();
        }
    };

    protected static final ImplBuilder viewDescriptorRegistryImplBuilder = new ImplBuilder() {
        public Object build( DefaultApplicationServices applicationServices ) {
            logger.info("Creating default service impl: ViewDescriptorRegistry");
            BeanFactoryViewDescriptorRegistry impl = new BeanFactoryViewDescriptorRegistry();
            impl.setApplicationContext(applicationServices.getApplicationContext());
            return impl;
        }
    };

    protected static final ImplBuilder labeledEnumResolverImplBuilder = new ImplBuilder() {
        public Object build( DefaultApplicationServices applicationServices ) {
            logger.info("Creating default service impl: LabeledEnumResolver");
            return new StaticLabeledEnumResolver();
        }
    };

    protected static final ImplBuilder messageSourceImplBuilder = new ImplBuilder() {
        public Object build( DefaultApplicationServices applicationServices ) {
            // The application context is our properly configured message source
            logger.info("Using MessageSource from application context");
            return applicationServices.getApplicationContext();
        }
    };

    protected static final ImplBuilder messageSourceAccessorImplBuilder = new ImplBuilder() {
        public Object build( DefaultApplicationServices applicationServices ) {
            // Just construct one on top of the current message source
            return new MessageSourceAccessor((MessageSource) applicationServices.getService(MessageSource.class));
        }
    };

    /**
     * Static initializer to construct the implementation builder map.
     */
    static {
        // Default service implementation builders
        serviceImplBuilders.put(ApplicationObjectConfigurer.class, applicationObjectConfigurerImplBuilder);
        serviceImplBuilders.put(ApplicationSecurityManager.class, applicationSecurityManagerImplBuilder);
        serviceImplBuilders.put(BinderSelectionStrategy.class, binderSelectionStrategyImplBuilder);
        serviceImplBuilders.put(BindingFactoryProvider.class, bindingFactoryProviderImplBuilder);
        serviceImplBuilders.put(CommandConfigurer.class, commandConfigurerImplBuilder);
        serviceImplBuilders.put(ComponentFactory.class, componentFactoryImplBuilder);
        serviceImplBuilders.put(ConversionService.class, conversionServiceImplBuilder);
        serviceImplBuilders.put(FormComponentInterceptorFactory.class, formComponentInterceptorFactoryImplBuilder);
        serviceImplBuilders.put(FormPropertyFaceDescriptorSource.class, formPropertyFaceDescriptorSourceImplBuilder);
        serviceImplBuilders.put(IconSource.class, iconSourceImplBuilder);
        serviceImplBuilders.put(ImageSource.class, imageSourceImplBuilder);
        serviceImplBuilders.put(LabeledEnumResolver.class, labeledEnumResolverImplBuilder);
        serviceImplBuilders.put(MessageSource.class, messageSourceImplBuilder);
        serviceImplBuilders.put(MessageSourceAccessor.class, messageSourceAccessorImplBuilder);
        serviceImplBuilders.put(RulesSource.class, rulesSourceImplBuilder);
        serviceImplBuilders.put(SecurityControllerManager.class, SecurityControllerManagerImplBuilder);
        serviceImplBuilders.put(ValueChangeDetector.class, valueChangeDetectorImplBuilder);
        serviceImplBuilders.put(ViewDescriptorRegistry.class, viewDescriptorRegistryImplBuilder);
    }
}