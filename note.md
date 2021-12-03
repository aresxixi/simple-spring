BeanDefinitionRegistry用来存放BeanDefinition的

scanner执行scan，把扫描到的beanDefinition放到BeanDefinitionRegistry里

重要步骤
（1）获取到bean的定义，即BeanDefinition
（2）执行refresh
refresh里的重要步骤
1、prepareRefresh：修改closed、active状态，将listener和event列表都设成空的
2.obtainFreshBeanFactory：在构造器内就已经实例化好了一个DefaultListableBeanFactory类型的BeanFactory，在obtainFreshBeanFactory方法内先给它设置了一个serializationId，
然后将其返回
3、prepareBeanFactory：给上一步设置的BeanFactory设置了一大推属性，并注册了四个内置的bean
4、postProcessBeanFactory：空方法，啥都没干
5、invokeBeanFactoryPostProcessors：调用BeanFactory的后处理器？
6、registerBeanPostProcessors：给Bean注册后处理器
7、initMessageSource：又给beanFactory注册了一个MessageSource类型的Bean
8、initApplicationEventMulticaster：又给beanFactory注册了一个ApplicationEventMulticaster类型的Bean
9、onRefresh：空实现
10、registerListeners：给Context的ApplicationEventMulticaster添加了一些ApplicationListener，然后判断当前是否有earlyApplicationEvents，
有就用ApplicationEventMulticaster调用ApplicationListener发送出去
11、finishBeanFactoryInitialization：真正实现bean的初始化
    给BeanFactory设置了一个ConversionService
    给beanFactory添加了一个EmbeddedValueResolver
    实例化所有LoadTimeWeaverAware类型的Bean
    把beanFactory的TempClassLoader设为空
    为了确保已被缓存的一些东西与其源保持一致，冻结了对源的修改
    调用beanFactory的preInstantiateSingletons方法，由beanFactory完成所有非延迟的、单例的bean的实例化

ApplicationContext <-- ConfigurableApplicationContext <-- AbstractApplicationContext

在这个简单的项目里，所有的ApplicationContext都是AbstractApplicationContext的子类
AbstractApplicationContext有两个直接子类
1、GenericApplicationContext <-- AnnotationConfigApplicationContext
                             <-- GenericGroovyApplicationContext
                             <-- GenericXmlApplicationContext
                             <-- StaticApplicationContext
1、AbstractRefreshableApplicationContext 
    <-- AbstractRefreshableApplicationContext 
        <-- AbstractRefreshableConfigApplicationContext 
            <-- AbstractXmlApplicationContext
                <-- ClassPathXmlApplicationContext
                <-- FileSystemXmlApplicationContext

BeanFactory的子类虽然多，但真正非抽象的、非ApplicationContext的只有三个
（1）SimpleJndiBeanFactory 基本没用到
（2）StaticListableBeanFactory 完全没用到
（3）DefaultListableBeanFactory
第一个基本没用到，第二个完全没用到，真正发挥作用的是第三个

DefaultListableBeanFactory创建bean，都是交个它自己的getBean方法完成的

重要方法getBean，但是这个方法的四个重载方法都是简单的调用了doGetBean，这个方法只有唯一实现，没有重载
getBean和doGetBean都定义在AbstractBeanFactory中
开始创建bean

doGetBean方法先尝试从缓存中获取bean，在初次获取的时候肯定是不存在的，接下来又尝试从父级BeanFactory中获取，很明显这是一个递归的过程，父级还有父级，所以这一段对于理解并没有意义，直接当它不会获取到，继续往下看。
两次想要偷懒都没得逞，beanFactory老实了，着手自己创建bean了。
创建bean第一步：取到beanName对应的BeanDefinition，此处先忽略一个细节：RootBeanDefinition
创建bean第二步：从BeanDefinition里获取到到bean依赖的那些bean的名字，然后挨个把这些依赖的bean get一遍，显然这里又递归了，不用管它，继续往下看
创建bean第三步：调用自己的getSingleton方法，注意getSingleton有多个重载方法，这里调用的是
    "getSingleton(String beanName, ObjectFactory<?> singletonFactory)"
这个方法的第二个参数在调用时以lambda形式传入了一段调用了createBean的逻辑，很明显创建bean的动作在这里边执行
createBean定义在beanFactory的父类AbstractAutowireCapableBeanFactory里
创建bean第四部：createBean里调用doCreateBean，doCreateBean里调用createBeanInstance，createBeanInstance里调用instantiateBean
instantiateBean会调用instantiationStrategy对象的instantiate方法，instantiate方法会调用BeanUtils.instantiateClass(constructorToUse)
已经很明显了，BeanUtils.instantiateClass里使用反射创建出实例

现在已经拿到实例了，我们从反方向看看后续的步骤做了什么
instantiationStrategy对象的instantiate方法创建实例后就直接返回了，然后instantiateBean方法继续执行
instantiateBean方法吧拿到的实例包装为一个BeanWrapper，然后将其返回到createBeanInstance方法，createBeanInstance没做任何处理，直接将其返回到doCreateBean里

doCreateBean拿到BeanWrapper，先执行populateBean，此处会将bean内需要Autowire的属性注入，然后执行initializeBean，在此方法内会先判断bean是不是各种Aware，如果是就调用相应的方法。
然后再判断bean是不是实现了InitializingBean接口，是的话就调用它的afterPropertiesSet方法（似乎还允许用户自定义init方法，如果有的话在这一步也会调用）。至此doCreateBean执行完毕，将bean返回到上一层。

调用doCreateBean的是createBean，它拿到bean什么都没做，直接返回给了调用它的getSingleton方法，getSingleton将bean加入到缓存，然后返回给了上层的doGetBean

doGetBean拿到bean之后把它交给了getObjectForBeanInstance，在里边主要是处理了一些关于FactoryBean的事：如果bean是一个FactoryBean，那么就用它生产出真正的bean，如果不是就直接返回自己
doGetBean再次拿到了bean，在最后的阶段，用TypeConverter处理了bean，接着将bean转回它原有的类型，返回给了getBean

还记得是谁调用了getBean么？是preInstantiateSingletons，现在又回到了这个方法里，在这里刚才被创建创建出来的bean会被从缓存里取出来，
挨个判断是不是实现了SmartInitializingSingleton接口，如果是的话就在bean上调用它的afterSingletonsInstantiated方法，不是的话就什么都不做。至此preInstantiateSingletons也执行完毕了，接下来返回到refresh方法内

终于到最后了，refresh的主要任务也都已完成，在执行完发送REFRESH事件广播、清空无用的缓存之后，refresh执行完毕，Spring的ApplicationContext启动完成。
