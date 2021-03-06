package minispring;

import minispring.aop.AdvisedSupport;
import minispring.aop.HealthMethodBeforeAdvice;
import minispring.aop.HealthService;
import minispring.aop.HealthServiceInterceptor;
import minispring.aop.IHealthService;
import minispring.aop.Pointcut;
import minispring.aop.TargetSource;
import minispring.aop.aspectj.ExpressionPointcut;
import minispring.aop.aspectj.ExpressionPointcutAdvisor;
import minispring.aop.framework.CglibAopProxy;
import minispring.aop.framework.JdkDynamicAopProxy;
import minispring.aop.framework.ProxyFactory;
import minispring.aop.framework.adapter.MethodBeforeInterceptor;
import minispring.autowired.AutowiredPersonDao;
import minispring.autowired.AutowiredPersonService;
import minispring.bean.NeoPersonService;
import minispring.bean.PersonDao;
import minispring.bean.PersonService;
import minispring.beans.BeanException;
import minispring.beans.PropertyValue;
import minispring.beans.PropertyValues;
import minispring.beans.factory.config.BeanDefinition;
import minispring.beans.factory.config.BeanReference;
import minispring.beans.factory.strategy.SimpleInstantiationStrategy;
import minispring.beans.factory.support.DefaultListableBeanFactory;
import minispring.beans.factory.xml.XmlBeanDefinitionReader;
import minispring.context.support.ClassPathXmlApplicationContext;
import minispring.listener.CustomizedEvent;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * @author lihua
 * @since 2021/8/23
 */
class FunctionTest {

    private static final String SERVICE_NAME = "personService";

    private static final String DAO_NAME = "personDao";

    private static final String PERSON_NAME = "喵喵";

    private static final Integer PERSON_GENDER = 0;

    private static final String CONFIG_LOCATION = "classpath:spring.xml";

    @Test
    @DisplayName("容器类功能-不存在bean定义")
    void testStep1() {
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        Assertions.assertThrows(BeanException.class, () -> beanFactory.getBean(SERVICE_NAME));
    }

    @Test
    @DisplayName("容器类功能-默认cglib方式创建bean")
    void testStep2() {
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        getAndCompareBean(beanFactory);
    }

    private void getAndCompareBean(DefaultListableBeanFactory beanFactory) {
        BeanDefinition beanDefinition = new BeanDefinition(PersonService.class);
        beanFactory.registerBeanDefinition(SERVICE_NAME, beanDefinition);
        PersonService newPersonService = (PersonService) beanFactory.getBean(SERVICE_NAME);
        Assertions.assertNotNull(newPersonService);
        Assertions.assertNull(newPersonService.getName());
        PersonService cachedPersonService = (PersonService) beanFactory.getBean(SERVICE_NAME);
        Assertions.assertEquals(newPersonService.hashCode(), cachedPersonService.hashCode());
    }

    @Test
    @DisplayName("容器类功能-jdk方式创建bean")
    void testStep3() {
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        beanFactory.setInstantiationStrategy(new SimpleInstantiationStrategy());
        getAndCompareBean(beanFactory);
    }

    /**
     * 使用到的构造方法：
     *
     * @see PersonService#PersonService(java.lang.String, java.lang.Integer)
     */
    @Test
    @DisplayName("有参构造方法")
    void testStep4() {
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        BeanDefinition beanDefinition = new BeanDefinition(PersonService.class);
        beanFactory.registerBeanDefinition(SERVICE_NAME, beanDefinition);
        PersonService newPersonService = (PersonService) beanFactory.getBean(SERVICE_NAME, PERSON_NAME, PERSON_GENDER);
        Assertions.assertNotNull(newPersonService);
        Assertions.assertEquals(PERSON_NAME, newPersonService.getName());
    }

    @Test
    @DisplayName("实例化后初始化属性")
    void testStep5() {
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        beanFactory.registerBeanDefinition(DAO_NAME, new BeanDefinition(PersonDao.class));
        BeanDefinition beanDefinition = getServiceBeanDefinition();
        beanFactory.registerBeanDefinition(SERVICE_NAME, beanDefinition);
        PersonService newPersonService = (PersonService) beanFactory.getBean(SERVICE_NAME);
        Assertions.assertNotNull(newPersonService);
        Assertions.assertEquals(PERSON_NAME, newPersonService.getName());
        Integer gender = newPersonService.getGender();
        Assertions.assertEquals(PERSON_GENDER, gender);
    }

    private BeanDefinition getServiceBeanDefinition() {
        PropertyValues propertyValues = new PropertyValues();
        propertyValues.addPropertyValue(new PropertyValue("name", PERSON_NAME));
        propertyValues.addPropertyValue(new PropertyValue("gender", PERSON_GENDER));
        propertyValues.addPropertyValue(new PropertyValue(DAO_NAME, new BeanReference(DAO_NAME)));
        return new BeanDefinition(PersonService.class, propertyValues);
    }

    @Test
    @DisplayName("xml配置对象信息")
    void testStep6() {
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        XmlBeanDefinitionReader beanDefinitionReader = new XmlBeanDefinitionReader(beanFactory);
        beanDefinitionReader.loadBeanDefinitions(CONFIG_LOCATION);
        Object bean = beanFactory.getBean(SERVICE_NAME);
        Assertions.assertNotNull(bean);
        Integer gender = ((PersonService) bean).queryGenderByName(PERSON_NAME);
        Assertions.assertEquals(PERSON_GENDER, gender);
    }

    @Test
    @DisplayName("applicationContext和postProcessor")
    void testStep7() {
        ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext(CONFIG_LOCATION);
        Object bean = applicationContext.getBean(SERVICE_NAME);
        Assertions.assertNotNull(bean);
        // postProcessors改变了属性
        Assertions.assertEquals("张喵喵", ((PersonService) bean).getName());
        Assertions.assertEquals(1, ((PersonService) bean).getGender());
        Integer gender = ((PersonService) bean).queryGenderByName(PERSON_NAME);
        Assertions.assertEquals(PERSON_GENDER, gender);
    }

    @Test
    @DisplayName("初始化方法和销毁方法&aware注入")
    void testStep8() {
        ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext(CONFIG_LOCATION);
        applicationContext.registerShutdownHook();
        Object bean = applicationContext.getBean(SERVICE_NAME);
        Assertions.assertNotNull(bean);
    }

    @Test
    @DisplayName("对象作用域-prototype")
    void testStep9() {
        ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext(CONFIG_LOCATION);
        applicationContext.registerShutdownHook();
        Object bean = applicationContext.getBean(DAO_NAME);
        Assertions.assertNotNull(bean);
        System.out.println("第一个获取的bean的hashCode是： " + bean.hashCode());
        Object newBean = applicationContext.getBean(DAO_NAME);
        Assertions.assertNotEquals(bean, newBean);
        System.out.println("第二个获取的bean的hashCode是： " + newBean.hashCode());
    }

    @Test
    void testHook() {
        Assertions.assertDoesNotThrow(() ->
                Runtime.getRuntime().addShutdownHook(new Thread(() -> System.out.println("close！"))));
    }

    @Test
    @DisplayName("FactoryBean")
    void testStep10() {
        ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext("classpath:spring-factory-bean.xml");
        applicationContext.registerShutdownHook();
        NeoPersonService personService = (NeoPersonService) applicationContext.getBean(SERVICE_NAME);
        Assertions.assertNotNull(personService);
        Integer gender = personService.queryGenderByName(PERSON_NAME);
        Assertions.assertEquals(PERSON_GENDER, gender);
    }

    @Test
    @DisplayName("事件发布和监听")
    void testStep11() {
        ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext("classpath:spring-listener.xml");
        Assertions.assertNotNull(applicationContext);
        applicationContext.registerShutdownHook();
        applicationContext.publishEvent(new CustomizedEvent(applicationContext, "你好~"));
    }

    @Test
    @DisplayName("aop第一步-散装-JDK")
    void testStep12() {
        AdvisedSupport advisedSupport = getAdvisedSupport();
        JdkDynamicAopProxy aopProxy = new JdkDynamicAopProxy(advisedSupport);
        IHealthService healthService = (IHealthService) aopProxy.getProxy();
        Boolean flag = healthService.healthCheck();
        Assertions.assertTrue(flag);
    }

    private AdvisedSupport getAdvisedSupport() {
        ExpressionPointcut expressionPointcut = new ExpressionPointcut("execution(* minispring.aop.IHealthService.*(..))");
        HealthServiceInterceptor personServiceInterceptor = new HealthServiceInterceptor();
        AdvisedSupport advisedSupport = new AdvisedSupport();
        advisedSupport.setTargetSource(new TargetSource(new HealthService()));
        advisedSupport.setMethodInterceptor(personServiceInterceptor);
        advisedSupport.setMethodMatcher(expressionPointcut);
        return advisedSupport;
    }

    @Test
    @DisplayName("aop第二步-散装-CGLIB")
    void testStep13() {
        AdvisedSupport advisedSupport = getAdvisedSupport();
        CglibAopProxy aopProxy = new CglibAopProxy(advisedSupport);
        IHealthService healthService = (IHealthService) aopProxy.getProxy();
        Boolean flag = healthService.healthCheck();
        Assertions.assertTrue(flag);
    }

    @Test
    @DisplayName("aop第三步-引入工厂模式")
    void testStep14() {
        IHealthService service = new HealthService();
        HealthMethodBeforeAdvice advice = new HealthMethodBeforeAdvice();
        String expression = "execution(* minispring.aop.IHealthService.*(..))";
        ExpressionPointcutAdvisor advisor = new ExpressionPointcutAdvisor();
        advisor.setExpression(expression);
        advisor.setAdvice(advice);
        Pointcut pointcut = advisor.getPointcut();
        if (pointcut.getClassFilter().matches(service.getClass())) {
            AdvisedSupport advisedSupport = new AdvisedSupport();
            advisedSupport.setTargetSource(new TargetSource(service));
            advisedSupport.setMethodInterceptor(new MethodBeforeInterceptor(advice));
            advisedSupport.setMethodMatcher(pointcut.getMethodMatcher());
            advisedSupport.setProxyTargetClass(true);
            ProxyFactory aopFactory = new ProxyFactory(advisedSupport);
            IHealthService proxy = (IHealthService) aopFactory.getProxy();
            Boolean flag = proxy.healthCheck();
            Assertions.assertTrue(flag);
        }
    }

    @Test
    @DisplayName("aop第四步-集成到容器中")
    void testStep15() {
        ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext("classpath:spring-aop.xml");
        applicationContext.registerShutdownHook();
        IHealthService healthService = (IHealthService) applicationContext.getBean("healthService");
        Assertions.assertNotNull(healthService);
        Boolean flag = healthService.healthCheck();
        Assertions.assertTrue(flag);
        String message = healthService.getMessage();
        Assertions.assertNotNull(message);
        System.out.println("注入到类里面的message是：" + message);
    }

    @Test
    @DisplayName("通过注解完成对象注册")
    void testStep16() {
        ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext("classpath:spring-scan.xml");
        applicationContext.registerShutdownHook();
        AutowiredPersonDao dao = (AutowiredPersonDao) applicationContext.getBean("autowiredPersonDao");
        Assertions.assertNotNull(dao);
        Integer gender = dao.queryGenderByName(PERSON_NAME);
        Assertions.assertNotNull(gender);
    }

    @Test
    @DisplayName("通过properties配置文件为对象注入属性")
    void testStep17() {
        ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext("classpath:spring-properties.xml");
        applicationContext.registerShutdownHook();
        AutowiredPersonDao dao = (AutowiredPersonDao) applicationContext.getBean("autowiredPersonDao");
        Assertions.assertNotNull(dao);
        String message = dao.getMessage();
        Assertions.assertNotNull(message);
    }

    @Test
    @DisplayName("通过@Value注解为对象注入属性")
    void testStep18() {
        ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext("classpath:spring-value.xml");
        applicationContext.registerShutdownHook();
        AutowiredPersonService service = (AutowiredPersonService) applicationContext.getBean("autowiredPersonService");
        Assertions.assertNotNull(service);
        String message = service.getMessageOfAnnotation();
        Assertions.assertNotNull(message);
        Integer gender = service.queryGenderByName(PERSON_NAME);
        Assertions.assertNotNull(gender);
    }
}
