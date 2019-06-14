








# spring cloud 中Feign的使用和工作原理分析



Feign是一款声明式的HTTP调用客户端，用于简化目前Rest接口调用操作，可以使调用HTTP接口像方法调用一样简单。





# 1.使用介绍

这部分主要包含

1.简单介绍feign的使用

2.介绍一些配置信息的

## 使用

pom依赖

```xml
<dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-openfeign</artifactId>
        </dependency>

```



@EnableFeignClients注解启用feign

```java
@SpringBootApplication
@EnableFeignClients
public class SpringDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringDemoApplication.class, args);
    }
}
```

@FeignClient注解配置http客户端

绝对地址

```java
@FeignClient(name = "api", url = "http://127.0.0.1:7333", path = "/api"
// , fallback = ApiFeignClientFallback.class
        , fallbackFactory = ApiFeignClientFallbackFactory.class
)
public interface ApiFeignClient {

    @GetMapping("/hello")
    String hello(@RequestParam("name") String name);


    @GetMapping("/timeout")
    String timeout();
}
```

注册中心

```java
@FeignClient(name = "eureka-feign", path = "/api")
public interface ApiFeignClient {

    @GetMapping("/hello")
    String hello(@RequestParam("name") String name);


    @GetMapping("/timeout")
    String timeout();

    @PostMapping("/post")
    User postMethod(@RequestBody User user);
}

```

配置文件 application.yml

```java
spring:
  profiles: dev0
  application:
    name: eureka-feign
server:
  port: 7200
eureka:
  client:
    serviceUrl:
      defaultZone: http://127.0.0.1:7700/eureka
```





使用

注入到其他的类中,调用方法,即可发送http请求

```java
    @Autowired
    private ApiFeignClient apiFeignClient;
```



## @EnableFeignClients 注解属性

在启动类上开启的@EnableFeignClients 注解



```java

 /**
 *可以通过value属性或basePackages属性来制定扫描的包路径。
 **/
 String[] value() default {};

 String[] basePackages() default {};

 /**
  *这个属性指定的class在的package会被扫描
  */
 Class<?>[] basePackageClasses() default {};

 /**
   *defaultConfiguration属性是可以定义全局Feign配置的类，默认使用FeignClientsConfiguration类
  */
 Class<?>[] defaultConfiguration() default {};

 /**
  *clients属性是精准指定Class扫描，如果这个属性不为空,关闭扫描
  */
 Class<?>[] clients() default {};
```



确定要扫描的包(类),默认为使用了注解的类所在包



## @FeignClient 注解属性

```java

 /**
 *value和name用于定义http客户端服务的名称,spring beanDefinition的name  如果要在spring cloud为配合Rinbon做服务间调用负载均衡的话。这里的name=注册在eurke上的目标服务application.name
 **/
 @AliasFor("name")
 String value() default "";

 @AliasFor("value")
 String name() default "";
 
 /**
  * qualifier属性在spring容器中定义FeignClient的bean时，配置名称，在装配bean的时候可以用这个名称装配。使用spring的注解：Qualifier。
  */
 String qualifier() default "";

 /**
  * url属性用来定义请求的绝对URL。
  */
 String url() default "";

 /**
  * 在客户端返回404时是进行decode操作还是抛出异常的标记。
  */
 boolean decode404() default false;

 /**
  * configuration属性，自定义配置类
在配置类中可以自己定义Feign请求的`Decoder`解码器、`Encoder`编码器、`Contract`组件扫描构造器。
默认使用FeignClientsConfiguration
  */
 Class<?>[] configuration() default {};

 /**
  * 使用fallback机制时可以配置的类属性，继承客户端接口，实现fallback逻辑。如果要使用fallback机制需要配合Hystrix一起，所以需要开启Hystrix。(默认关闭) feign.hystrix.enabled=true 
  同时配置 fallback 和fallbackFactory 属性 使用fallback
  */
 Class<?> fallback() default void.class;

 /**
  * fallbackFactory属性 生产fallback实例，生产的自然是继承客户端接口的实例。
  */
 Class<?> fallbackFactory() default void.class;

 /**
  * path属性 每个接口url的统一前缀
  */
 String path() default "";

 /**
  *primary属性 标记在spring容器中为primary bean
  */
 boolean primary() default true;
```



fallback 和 fallbackFactory 两者主要差别在于 fallbackFactory 可以获取到进入fallback异常

# 2.原理解析



### 初始化流程



spring cloud feign 在启动的时候会加载几个配置类



#### 1.FeignClientsConfiguration

加载Decoder、Encoder、Retryer、Contract（SpringMvcContract）、FeignBuilder等组件

其中Decoder 和 Encoder 默认使用的是spring mvc的方式 默认通过HttpMessageConverters进行处理

```java
@Configuration
public class FeignClientsConfiguration {

    @Autowired
    private ObjectFactory<HttpMessageConverters> messageConverters;

   //...代码省略

    @Bean
    @ConditionalOnMissingBean
    public Decoder feignDecoder() {
        return new OptionalDecoder(new ResponseEntityDecoder(new               SpringDecoder(this.messageConverters)));
    }
    //容器默认注入了SpringEncoder作为系统的编码器，使用Spring MVC的 messageConverters。
    @Bean
    @ConditionalOnMissingBean
    public Encoder feignEncoder() {
        return new SpringEncoder(this.messageConverters);
    }
    //注入了SpringMvcContract这个类作为对Spring MVC的注解解析
    @Bean
    @ConditionalOnMissingBean
    public Contract feignContract(ConversionService feignConversionService) {
        return new SpringMvcContract(this.parameterProcessors, feignConversionService);
    }

    @Bean
    public FormattingConversionService feignConversionService() {
        FormattingConversionService conversionService = new DefaultFormattingConversionService();
        for (FeignFormatterRegistrar feignFormatterRegistrar : feignFormatterRegistrars) {
            feignFormatterRegistrar.registerFormatters(conversionService);
        }
        return conversionService;
    }

    @Configuration
    @ConditionalOnClass({ HystrixCommand.class, HystrixFeign.class })
    protected static class HystrixFeignConfiguration {
        @Bean
        @Scope("prototype")
        @ConditionalOnMissingBean
        @ConditionalOnProperty(name = "feign.hystrix.enabled")
        public Feign.Builder feignHystrixBuilder() {
            return HystrixFeign.builder();
        }
    }

    @Bean
    @ConditionalOnMissingBean
    public Retryer feignRetryer() {
        return Retryer.NEVER_RETRY;
    }
	
    //feign代理对象的构建类,包含构建代理对象的所有属性
    @Bean
    @Scope("prototype")
    @ConditionalOnMissingBean
    public Feign.Builder feignBuilder(Retryer retryer) {
        return Feign.builder().retryer(retryer);
    }

    @Bean
    @ConditionalOnMissingBean(FeignLoggerFactory.class)
    public FeignLoggerFactory feignLoggerFactory() {
        return new DefaultFeignLoggerFactory(logger);
    }

}

```





#### 2.FeignAutoConfiguration

在这个自动装配类中主要声明了Feign上下文（FeignContext）、Targeter、Client(仅仅组件)等组件

```java
@Configuration
@ConditionalOnClass(Feign.class)
@EnableConfigurationProperties({FeignClientProperties.class, FeignHttpClientProperties.class})
public class FeignAutoConfiguration {

    @Autowired(required = false)
    private List<FeignClientSpecification> configurations = new ArrayList<>();

    @Bean
    public HasFeatures feignFeature() {
        return HasFeatures.namedFeature("Feign", Feign.class);
    }

    //FeignContext 继承自NamedContextFactory，
    //FeignContext用于客户端配置类独立注册
    //例如 不同的客户端指定不同配置类，就需要对配置类进行隔离，FeignContext就是用于隔离配置的。
     @Bean
    public FeignContext feignContext() {
        FeignContext context = new FeignContext();
        context.setConfigurations(this.configurations);
        return context;
    }
    //默认引入的包依赖已带此类，所以默认使用的Targeter是这个带熔断的HystrixTargeter
    @Configuration
    @ConditionalOnClass(name = "feign.hystrix.HystrixFeign")
    protected static class HystrixFeignTargeterConfiguration {
        @Bean
        @ConditionalOnMissingBean
        public Targeter feignTargeter() {
            return new HystrixTargeter();
        }
    }
      //Targeter 实例化的入口类
    //默认的Targeter实现  需要排除hystrix 相关依赖
    @Configuration
    @ConditionalOnMissingClass("feign.hystrix.HystrixFeign")
    protected static class DefaultFeignTargeterConfiguration {
        @Bean
        @ConditionalOnMissingBean
        public Targeter feignTargeter() {
            return new DefaultTargeter();
        }
    }
 
        //
        // client 的 httpclinet库实现
    @Configuration
    @ConditionalOnClass(ApacheHttpClient.class)
    @ConditionalOnMissingClass("com.netflix.loadbalancer.ILoadBalancer")
    @ConditionalOnMissingBean(CloseableHttpClient.class)
    @ConditionalOnProperty(value = "feign.httpclient.enabled", matchIfMissing = true)
    protected static class HttpClientFeignConfiguration {
        //...代码省略
    }
    
      // client 的 OkHttpClient库实现
    @Configuration
    @ConditionalOnClass(OkHttpClient.class)
    @ConditionalOnMissingClass("com.netflix.loadbalancer.ILoadBalancer")
    @ConditionalOnMissingBean(okhttp3.OkHttpClient.class)
    @ConditionalOnProperty(value = "feign.okhttp.enabled")
    protected static class OkHttpFeignConfiguration {
        //...代码省略
    }

}

```





##### FeignContext 隔离配置

在@FeignClient注解参数configuration，指定的类是Spring的Configuration Bean，里面方法上加@Bean注解实现Bean的注入，可以指定feign客户端的各种配置，包括Encoder/Decoder/Contract/Feign.Builder等。不同的客户端指定不同配置类，就需要对配置类进行隔离，FeignContext就是用于隔离配置的。

```java
public class FeignContext extends NamedContextFactory<FeignClientSpecification> {

    public FeignContext() {
        super(FeignClientsConfiguration.class, "feign", "feign.client.name");
    }
}
```

FeignContext继承NamedContextFactory，空参数构造函数指定FeignClientsConfiguration类为默认配置。
NamedContextFactory实现接口ApplicationContextAware，注入ApplicationContext作为parent：

```java
public abstract class NamedContextFactory<C extends NamedContextFactory.Specification>
        implements DisposableBean, ApplicationContextAware {
    //每个@FeignClent的name 对应一个Context
    private Map<String, AnnotationConfigApplicationContext> contexts = new ConcurrentHashMap<>();
    //所有configuration的集合 key为@FeignClent的name
    private Map<String, C> configurations = new ConcurrentHashMap<>();
    //父ApplicationContext，通过ApplicationContextAware接口注入
    private ApplicationContext parent;
    //默认配置类
    private Class<?> defaultConfigType;
    private final String propertySourceName;
    private final String propertyName;
。。。
    //设置配置，在FeignAutoConfiguration中将Spring Context中的所有FeignClientSpecification设置进来，如果@EnableFeignClients有设置参数defaultConfiguration也会加进来，前面已经分析在registerDefaultConfiguration方法中注册的FeignClientSpecification Bean
    public void setConfigurations(List<C> configurations) {
        for (C client : configurations) {
            this.configurations.put(client.getName(), client);
        }
    }

    //获取指定@FeignClent的name的ApplicationContext，先从缓存中获取，没有就创建
    protected AnnotationConfigApplicationContext getContext(String name) {
        if (!this.contexts.containsKey(name)) {
            synchronized (this.contexts) {
                if (!this.contexts.containsKey(name)) {
                    this.contexts.put(name, createContext(name));
                }
            }
        }
        return this.contexts.get(name);
    }

    //创建ApplicationContext
    protected AnnotationConfigApplicationContext createContext(String name) {
        //新建AnnotationConfigApplicationContext
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        //根据name在configurations找到所有的配置类，注册到context总
        if (this.configurations.containsKey(name)) {
            for (Class<?> configuration : this.configurations.get(name)
                    .getConfiguration()) {
                context.register(configuration);
            }
        }
        //将default.开头的默认默认也注册到Context中
        for (Map.Entry<String, C> entry : this.configurations.entrySet()) {
            if (entry.getKey().startsWith("default.")) {
                for (Class<?> configuration : entry.getValue().getConfiguration()) {
                    context.register(configuration);
                }
            }
        }
        //注册一些需要的bean
        context.register(PropertyPlaceholderAutoConfiguration.class,
                this.defaultConfigType);
        context.getEnvironment().getPropertySources().addFirst(new MapPropertySource(
                this.propertySourceName,
                Collections.<String, Object> singletonMap(this.propertyName, name)));
        if (this.parent != null) {
        // 设置parent
            context.setParent(this.parent);
        }
        //刷新，完成配置类中的bean生成
        context.refresh();
        return context;
    }

    //从命名空间中获取指定类型的Bean
    public <T> T getInstance(String name, Class<T> type) {
        AnnotationConfigApplicationContext context = getContext(name);
        if (BeanFactoryUtils.beanNamesForTypeIncludingAncestors(context,
                type).length > 0) {
            return context.getBean(type);
        }
        return null;
    }

    //从命名空间中获取指定类型的Bean
    public <T> Map<String, T> getInstances(String name, Class<T> type) {
        AnnotationConfigApplicationContext context = getContext(name);
        if (BeanFactoryUtils.beanNamesForTypeIncludingAncestors(context,
                type).length > 0) {
            return BeanFactoryUtils.beansOfTypeIncludingAncestors(context, type);
        }
        return null;
    }

}
```



关键的方法是createContext，为@FeignClient的name独立创建ApplicationContext，设置parent为外部传入的Context，这样就可以共用外部的Context中的Bean，又有各种独立的配置Bean

从FeignContext中获取Bean，需要传入@FeignClient的name，根据name找到缓存中的ApplicationContext，先从自己注册的Bean中获取bean，没有获取到再从到parent中获取。







#### 3.@EnableFeignClients

Feign的使用是从`@EnableFeignClients`注解开始的，注解源码如下：

```java
@Import(FeignClientsRegistrar.class)
public @interface EnableFeignClients {
    ...
}
```


在注解上有一个关键注解`@Import(FeignClientsRegistrar.class)`，导入了Feign组件的注册器，用于扫描Feign组件与初始化Feign组件的Bean定义信息.

FeignClientsRegistrar实现了ImportBeanDefinitionRegistrar,ImportBeanDefinitionRegistrar接口是Spring的一个扩展接口，通过registerBeanDefinitions方法向容器中注册Bean。

```java
class FeignClientsRegistrar implements ImportBeanDefinitionRegistrar,
  ResourceLoaderAware, BeanClassLoaderAware, EnvironmentAware {
    ...
 @Override
 public void registerBeanDefinitions(AnnotationMetadata metadata,
   BeanDefinitionRegistry registry) {
        //注册`@EnableFeignClients `注解中`defaultConfiguration `属性
        //注册的bean name 为 "default." + metadata.getClassName();
  registerDefaultConfiguration(metadata, registry);
        //注册@FeignClient 注解的接口
  registerFeignClients(metadata, registry);
 }
 ...
}
```

其中`registerDefaultConfiguration `方法注册了`@EnableFeignClients `注解中`defaultConfiguration `属性，重点我们看`registerFeignClients `方法。

```java
public void registerFeignClients(AnnotationMetadata metadata,
   BeanDefinitionRegistry registry) {
  ClassPathScanningCandidateComponentProvider scanner = getScanner();
  scanner.setResourceLoader(this.resourceLoader);

  Set<String> basePackages;
  //取出EnableFeignClients注解中的信息
  Map<String, Object> attrs = metadata
    .getAnnotationAttributes(EnableFeignClients.class.getName());
  //这里定义了注解的过滤器，只有@FeignClient注解才会被筛出来
  AnnotationTypeFilter annotationTypeFilter = new AnnotationTypeFilter(
    FeignClient.class);
  //查看@EnableFeignClients中设置的clients参数
  final Class<?>[] clients = attrs == null ? null
    : (Class<?>[]) attrs.get("clients");
  //设置过滤器和设置要扫描的包
  //如果没有设置clients参数，那么设置要扫描的包是@EnableFeignClients中设置的value值
  if (clients == null || clients.length == 0) {
      //设置过滤器
     scanner.addIncludeFilter(annotationTypeFilter);
     basePackages = getBasePackages(metadata);
  }
  else {
   final Set<String> clientClasses = new HashSet<>();
   basePackages = new HashSet<>();
   for (Class<?> clazz : clients) {
    basePackages.add(ClassUtils.getPackageName(clazz));
    clientClasses.add(clazz.getCanonicalName());
   }
   AbstractClassTestingTypeFilter filter = new AbstractClassTestingTypeFilter() {
    @Override
    protected boolean match(ClassMetadata metadata) {
     String cleaned = metadata.getClassName().replaceAll("\\$", ".");
     return clientClasses.contains(cleaned);
    }
   };
   scanner.addIncludeFilter(
     new AllTypeFilter(Arrays.asList(filter, annotationTypeFilter)));
  }

  for (String basePackage : basePackages) {
   //扫描设置的包中的@FeignClient注解
   Set<BeanDefinition> candidateComponents = scanner
     .findCandidateComponents(basePackage);
   for (BeanDefinition candidateComponent : candidateComponents) {
    if (candidateComponent instanceof AnnotatedBeanDefinition) {
     // 验证@ FeignClient注解的类是一个接口
     AnnotatedBeanDefinition beanDefinition = (AnnotatedBeanDefinition) candidateComponent;
     AnnotationMetadata annotationMetadata = beanDefinition.getMetadata();
     Assert.isTrue(annotationMetadata.isInterface(),
       "@FeignClient can only be specified on an interface");
     //获得@ FeignClient所设置的属性
     Map<String, Object> attributes = annotationMetadata
       .getAnnotationAttributes(
         FeignClient.class.getCanonicalName());
     //获取clientName 从@FeignClient中设置的value,name,serviceId值中获取
     String name = getClientName(attributes);
     //将@FeignClient注解中Configuration属性注册进容器中，其中名字是name的值
     registerClientConfiguration(registry, name,
       attributes.get("configuration"));
        
     //往ioc容器中注册
     //下面有讲解
     registerFeignClient(registry, annotationMetadata, attributes);
    }
   }
  }
 }

```

`registerFeignClient `方法讲解，其实这一块就是将`@FeignClient `注解中所有的value值取出并且放入容器中。**在这里我们需要注意此时注册是FeignClientFactoryBean类型，之前注册的配置类都是以FeignClientSpecification类型注册**



```java
private void registerFeignClient(BeanDefinitionRegistry registry,
   AnnotationMetadata annotationMetadata, Map<String, Object> attributes) {
  String className = annotationMetadata.getClassName();
    //创建FeignClientFactoryBean 类型的BeanDefinition
  BeanDefinitionBuilder definition = BeanDefinitionBuilder
    .genericBeanDefinition(FeignClientFactoryBean.class);
  validate(attributes);
  definition.addPropertyValue("url", getUrl(attributes));
  definition.addPropertyValue("path", getPath(attributes));
  String name = getName(attributes);
  definition.addPropertyValue("name", name);
  definition.addPropertyValue("type", className);
  definition.addPropertyValue("decode404", attributes.get("decode404"));
  definition.addPropertyValue("fallback", attributes.get("fallback"));
  definition.addPropertyValue("fallbackFactory", attributes.get("fallbackFactory"));
  definition.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_BY_TYPE);

  String alias = name + "FeignClient";
  AbstractBeanDefinition beanDefinition = definition.getBeanDefinition();

  boolean primary = (Boolean)attributes.get("primary"); // has a default, won't be null

  beanDefinition.setPrimary(primary);

  String qualifier = getQualifier(attributes);
  if (StringUtils.hasText(qualifier)) {
   alias = qualifier;
  }

  BeanDefinitionHolder holder = new BeanDefinitionHolder(beanDefinition, className,
    new String[] { alias });
  BeanDefinitionReaderUtils.registerBeanDefinition(holder, registry);
 }

```



#### 回顾

大概总结一下FeignClient的注册流程

1. 扫描`@EnableFeignClients`注解中,确定要扫描的basePackage,扫描包中所有使用了`@FeignClient`的接口
2. 读取接口上面的 `@FeignClient` 注解参数
3. 如果此接口上有Configuration参数，那么先进行注册此参数，注意此参数注册在Spring容器中是以`FeignClientSpecification`类型注册的
4. 注册完Configuration参数以后，然后将其余的信息注册到容器中，注意这时是以`FeignClientFactoryBean `类型注册的，另外此时的Configuration参数并没有传过来。





### 方法执行流程

这部分包含

1.代理对像创建过程

2.具体方法执行(代理对像执行方法)



#### 1.代理对像创建过程

接下来可以查看Feign代理Bean实例是如何创建的的，参见FeignClientFactoryBean源码：

作为一个实现了FactoryBean的工厂类，那么每次在Spring 实例化beansh会调用它的getObject()方法。

```java
class FeignClientFactoryBean implements FactoryBean<Object>, InitializingBean,
        ApplicationContextAware {
        
    //这里的变量对应@feignClient注解中的属性
    private Class<?> type;
    private String name;
    private String url;
    private String path;
    private boolean decode404;
    private ApplicationContext applicationContext;
    private Class<?> fallback = void.class;
    private Class<?> fallbackFactory = void.class;
    -------其余代码省略
 
    @Override
    public Object getObject() throws Exception {
        return getTarget();
    }
 
    <T> T getTarget() {
        //FeignContext在FeignAutoConfiguration中自动注册，FeignContext用于客户端配置类独立注册，后面具体分析
        FeignContext context = applicationContext.getBean(FeignContext.class);
        //创建Feign.Builder 从FeignContext获取每个FeignClient中Encoder Decoder Contract,FeignClientProperties配置,等组件并设置到Builder中
        Feign.Builder builder = feign(context);
        //@FeignClient注解没有配置URL属性
        if (!StringUtils.hasText(this.url)) {
            String url;
            if (!this.name.startsWith("http")) {
                url = "http://" + this.name;
            }
            else {
                url = this.name;
            }
            url += cleanPath();
            return loadBalance(builder, context, new HardCodedTarget<>(this.type,
                    this.name, url));
        }
        if (StringUtils.hasText(this.url) && !this.url.startsWith("http")) {
            this.url = "http://" + this.url;
        }
        String url = this.url + cleanPath();
        //获取网络请求客户端：Spring封装了基于Ribbon的客户端（LoadBalancerFeignClient）
        //1、Feign自己封装的Request（基于java.net原生），2、OkHttpClient（新一代/HTTP2），3、ApacheHttpClient（常规）
        Client client = getOptional(context, Client.class);
        if (client != null) {
            if (client instanceof LoadBalancerFeignClient) {
                // not lod balancing because we have a url,
                // but ribbon is on the classpath, so unwrap
                client = ((LoadBalancerFeignClient)client).getDelegate();
            }
             //设置调用客户端
            builder.client(client);
        }
        //DefaultTargeter或者HystrixTargeter(FeignAutoConfiguration)，其中HystrixTargeter带熔断和降级功能主要用在Builder中配置调用失败回调方法
        Targeter targeter = get(context, Targeter.class);
 
        //Bean实例化,最终生成的动态代理类(jdk的动态代理方式InvocationHandler)
        return targeter.target(this, builder, context, new HardCodedTarget<>(
                this.type, this.name, url));
    }
    -------其余代码省略
}

```



我们可以看getObject()最后一句可以看到返回了Targeter.target的方法。我们在之前的`FeignAutoConfiguration`类，可以看到其中有两个`Targeter`类，一个是`DefaultTargeter`，一个是`HystrixTargeter`。我们以`DefaultTargeter`为例介绍一下是如何通过创建代理对象的

```java
class DefaultTargeter implements Targeter {

    @Override
    public <T> T target(FeignClientFactoryBean factory, Feign.Builder feign, FeignContext context,
                        Target.HardCodedTarget<T> target) {
        return feign.target(target);
 
    }
}
```



```java


//feign.Feign.Builder
public static class Builder {
    -------其余代码省略
     public <T> T target(Target<T> target) {
      return build().newInstance(target);
    }

    public Feign build() {
      SynchronousMethodHandler.Factory synchronousMethodHandlerFactory =
          new SynchronousMethodHandler.Factory(client, retryer, requestInterceptors, logger,
                                               logLevel, decode404, closeAfterDecode);
      ParseHandlersByName handlersByName =
          new ParseHandlersByName(contract, options, encoder, decoder, queryMapEncoder,
                                  errorDecoder, synchronousMethodHandlerFactory);
      return new ReflectiveFeign(handlersByName, invocationHandlerFactory, queryMapEncoder);
    }
    -------其余代码省略
}

 
```

最终调用`ReflectiveFeign`类中`newInstance`方法是返回一个代理对象

```java
public class ReflectiveFeign extends Feign {
    -------其余代码省略
    public <T> T newInstance(Target<T> target) {
      //核心方法，解析定义的@FeignClient组件中的方法和请求路径 为每个方法创建一个MethodHandler
     //这里有用到springContract 解析springmvc的注解 获取到方法的元信息
     //key 接口类的方法名
    Map<String, MethodHandler> nameToHandler = targetToHandlersByName.apply(target);
    Map<Method, MethodHandler> methodToHandler = new LinkedHashMap<Method, MethodHandler>();
    List<DefaultMethodHandler> defaultMethodHandlers = new LinkedList<DefaultMethodHandler>();

    for (Method method : target.type().getMethods()) {
      if (method.getDeclaringClass() == Object.class) {
        continue;
      } else if(Util.isDefault(method)) {
        DefaultMethodHandler handler = new DefaultMethodHandler(method);
        defaultMethodHandlers.add(handler);
        methodToHandler.put(method, handler);
      } else {
        methodToHandler.put(method, nameToHandler.get(Feign.configKey(target.type(), method)));
      }
    }
    //创建InvocationHandler 对象
    //这里创建的是  new ReflectiveFeign.FeignInvocationHandler(target, dispatch); 对象
    InvocationHandler handler = factory.create(target, methodToHandler);
    //创建动态代理对象
    T proxy = (T) Proxy.newProxyInstance(target.type().getClassLoader(), new Class<?>[]{target.type()}, handler);

    for(DefaultMethodHandler defaultMethodHandler : defaultMethodHandlers) {
      defaultMethodHandler.bindTo(proxy);
    }
    return proxy;
  }
    -------其余代码省略
}
```







#### 2.请求方法执行



动态代理对象执行方法的时候都会调用到InvocationHandler.invoke()方法,而这里FeignInvocationHandler类的invoke中会根据method获取对应的`SynchronousMethodHandler`执行其 invoke 方法

```java
static class FeignInvocationHandler implements InvocationHandler {
    private final Target target;
    private final Map<Method, MethodHandler> dispatch;
 
    FeignInvocationHandler(Target target, Map<Method, MethodHandler> dispatch) {
      this.target = checkNotNull(target, "target");
      this.dispatch = checkNotNull(dispatch, "dispatch for %s", target);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
     //省略 equals hashCode 等方法的处理...
        
      //根据method 委托对应的  MethodHandler 执行
      return dispatch.get(method).invoke(args);
    }

 
  }
```



```java
final class SynchronousMethodHandler implements MethodHandler {

  private static final long MAX_RESPONSE_BUFFER_SIZE = 8192L;

  private final MethodMetadata metadata;
  private final Target<?> target;
  private final Client client;
  private final Retryer retryer;
  private final List<RequestInterceptor> requestInterceptors;
  private final Logger logger;
  private final Logger.Level logLevel;
  private final RequestTemplate.Factory buildTemplateFromArgs;
  private final Options options;
  private final Decoder decoder;
  private final ErrorDecoder errorDecoder;
  private final boolean decode404;
  private final boolean closeAfterDecode;


  @Override
  public Object invoke(Object[] argv) throws Throwable {
    //RequestTemplate 处理请求参数
    RequestTemplate template = buildTemplateFromArgs.create(argv);
    Retryer retryer = this.retryer.clone();
    while (true) {
      try {
        //执行请求并解析
        return executeAndDecode(template);
      } catch (RetryableException e) {
        retryer.continueOrPropagate(e);
        if (logLevel != Logger.Level.NONE) {
          logger.logRetry(metadata.configKey(), logLevel);
        }
        continue;
      }
    }
  }

  Object executeAndDecode(RequestTemplate template) throws Throwable {
    Request request = targetRequest(template);

    if (logLevel != Logger.Level.NONE) {
      logger.logRequest(metadata.configKey(), logLevel, request);
    }

    Response response;
    long start = System.nanoTime();
    try {
      //执行请求
      response = client.execute(request, options);
      // ensure the request is set. TODO: remove in Feign 10
      response.toBuilder().request(request).build();
    } catch (IOException e) {
      if (logLevel != Logger.Level.NONE) {
        logger.logIOException(metadata.configKey(), logLevel, e, elapsedTime(start));
      }
      throw errorExecuting(request, e);
    }
    long elapsedTime = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);

    boolean shouldClose = true;
    try {
      if (logLevel != Logger.Level.NONE) {
        response =
            logger.logAndRebufferResponse(metadata.configKey(), logLevel, response, elapsedTime);
        // ensure the request is set. TODO: remove in Feign 10
        response.toBuilder().request(request).build();
      }
      if (Response.class == metadata.returnType()) {
        if (response.body() == null) {
          return response;
        }
        if (response.body().length() == null ||
                response.body().length() > MAX_RESPONSE_BUFFER_SIZE) {
          shouldClose = false;
          return response;
        }
        // Ensure the response body is disconnected
        byte[] bodyData = Util.toByteArray(response.body().asInputStream());
        return response.toBuilder().body(bodyData).build();
      }
      if (response.status() >= 200 && response.status() < 300) {
        if (void.class == metadata.returnType()) {
          return null;
        } else {
          Object result = decode(response);
          shouldClose = closeAfterDecode;
          return result;
        }
      } else if (decode404 && response.status() == 404 && void.class != metadata.returnType()) {
        Object result = decode(response);
        shouldClose = closeAfterDecode;
        return result;
      } else {
        throw errorDecoder.decode(metadata.configKey(), response);
      }
    } catch (IOException e) {
      if (logLevel != Logger.Level.NONE) {
        logger.logIOException(metadata.configKey(), logLevel, e, elapsedTime);
      }
      throw errorReading(request, response, e);
    } finally {
      if (shouldClose) {
        ensureClosed(response.body());
      }
    }
  }
```







默认的client

```java
 public static class Default implements Client {

    private final SSLSocketFactory sslContextFactory;
    private final HostnameVerifier hostnameVerifier;

    /**
     * Null parameters imply platform defaults.
     */
    public Default(SSLSocketFactory sslContextFactory, HostnameVerifier hostnameVerifier) {
      this.sslContextFactory = sslContextFactory;
      this.hostnameVerifier = hostnameVerifier;
    }

    @Override
    public Response execute(Request request, Options options) throws IOException {
      HttpURLConnection connection = convertAndSend(request, options);
      return convertResponse(connection).toBuilder().request(request).build();
    }
 }
```



####  回顾

简单总结下工作原理

1.在初始化过程中@FeignClient接口以FeignClientFactoryBean类型注册IOC容器中

2.FeignClientFactoryBean.getObject() 方法创建FeignClient的jdk动态代理对象,其中会为接口中的每个方法创建一个MethodHandler对象

3.当调用方法时,会调用mehod对应的MethodHandler对象的invoke()方法

4.MethodHandler对象的invoke()方法会 处理请求参数,使用client 进行网络请求,处理http响应信息,并返回结果



![1](https://image-static.segmentfault.com/377/707/3777075039-5b042bdb65b51)



以上就是不整合Hytrix和RIbbon的feign的基本原理,下面简单介绍下Hytrix和RIbbon的整合



### Hytrix和RIbbon整合



#### hystrix整合

在spring could 中feign也整合了Hystrix，实现熔断降级的功能，在上面的分析中我们知道了feign在方法调用的时候会经过统一方法拦截器FeignInvocationHandler的处理，而在启用hystrix功能后是使用HystrixInvocationHandler代替



```java
final class HystrixInvocationHandler implements InvocationHandler {
	private final Target<?> target;
  private final Map<Method, MethodHandler> dispatch;
  private final FallbackFactory<?> fallbackFactory; // Nullable
  private final Map<Method, Method> fallbackMethodMap;
  private final Map<Method, Setter> setterMethodMap;
  
  //其他代码省略...
  
   @Override
  public Object invoke(final Object proxy, final Method method, final Object[] args)
      throws Throwable {
  	//省略 equals hashCode 等方法的处理...
		
    //hystrix 命令
    HystrixCommand<Object> hystrixCommand = new HystrixCommand<Object>(setterMethodMap.get(method)) {
      @Override
      protected Object run() throws Exception {
        try {
            //方法执行放在了hystrix 命令中
          return HystrixInvocationHandler.this.dispatch.get(method).invoke(args);
        } catch (Exception e) {
          throw e;
        } catch (Throwable t) {
          throw (Error) t;
        }
      }

      @Override
      protected Object getFallback() {
        if (fallbackFactory == null) {
          return super.getFallback();
        }
        try {
          Object fallback = fallbackFactory.create(getExecutionException());
          Object result = fallbackMethodMap.get(method).invoke(fallback, args);
          if (isReturnsHystrixCommand(method)) {
            return ((HystrixCommand) result).execute();
          } else if (isReturnsObservable(method)) {
            // Create a cold Observable
            return ((Observable) result).toBlocking().first();
          } else if (isReturnsSingle(method)) {
            // Create a cold Observable as a Single
            return ((Single) result).toObservable().toBlocking().first();
          } else if (isReturnsCompletable(method)) {
            ((Completable) result).await();
            return null;
          } else {
            return result;
          }
        } catch (IllegalAccessException e) {
          // shouldn't happen as method is public due to being an interface
          throw new AssertionError(e);
        } catch (InvocationTargetException e) {
          // Exceptions on fallback are tossed by Hystrix
          throw new AssertionError(e.getCause());
        }
      }
    };

    if (Util.isDefault(method)) {
      return hystrixCommand.execute();
    } else if (isReturnsHystrixCommand(method)) {
      return hystrixCommand;
    } else if (isReturnsObservable(method)) {
      // Create a cold Observable
      return hystrixCommand.toObservable();
    } else if (isReturnsSingle(method)) {
      // Create a cold Observable as a Single
      return hystrixCommand.toObservable().toSingle();
    } else if (isReturnsCompletable(method)) {
      return hystrixCommand.toObservable().toCompletable();
    }
    return hystrixCommand.execute();
  }
  
   //其他代码省略...
}
```



hystrix相关使用和原理就不在这里详细描述了。

#### Robbin整合

如果包含ribbon相关的包,FeignRibbonClientAutoConfiguration会自动装配LoadBalancer相关的client 

如LoadBalancerFeignClient

```java
public class LoadBalancerFeignClient implements Client {
。。。
    @Override
    public Response execute(Request request, Request.Options options) throws IOException {
        try {
            //获取URI
            URI asUri = URI.create(request.url());
            //获取客户端的名称
            String clientName = asUri.getHost();
            URI uriWithoutHost = cleanUrl(request.url(), clientName);
            //创建RibbonRequest
            FeignLoadBalancer.RibbonRequest ribbonRequest = new FeignLoadBalancer.RibbonRequest(
                    this.delegate, request, uriWithoutHost);
            //包装请求配置
            IClientConfig requestConfig = getClientConfig(options, clientName);
            //获取FeignLoadBalancer，替换请求中的客户端名称为实际ip, 发送网络请求，转换Response
            return lbClient(clientName).executeWithLoadBalancer(ribbonRequest,
                    requestConfig).toResponse();
        } catch (ClientException e) {
            。。。
        }
    }
```

Ribbon的相关使用和原理就不在这里详细描述了。



# 完