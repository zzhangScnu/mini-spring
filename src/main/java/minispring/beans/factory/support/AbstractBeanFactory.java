package minispring.beans.factory.support;

import lombok.Getter;
import minispring.beans.factory.FactoryBean;
import minispring.beans.factory.config.BeanDefinition;
import minispring.beans.factory.config.BeanPostProcessor;
import minispring.beans.factory.config.ConfigurableBeanFactory;
import minispring.util.ClassUtils;
import minispring.util.StringValueResolver;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 本类实现了ConfigurableBeanFactory接口，并继承了DefaultSingletonBeanRegistry类
 * 其中，destroySingletons由接口定义，由父类实现
 * ConfigurableBeanFactory和DefaultSingletonBeanRegistry并没有直接的实现关系，但是通过本类，达到了实现的效果，是一种不错的分层和隔离
 *
 * @author lihua
 * @since 2021/8/25
 */
@Getter
public abstract class AbstractBeanFactory extends FactoryBeanRegistrySupport implements ConfigurableBeanFactory {

    private final ClassLoader classLoader = ClassUtils.getDefaultClassLoader();

    private final List<BeanPostProcessor> beanPostProcessors = new ArrayList<>();

    private final List<StringValueResolver> embeddedStringValueResolvers = new ArrayList<>();

    @Override
    public Object getBean(String name, Object... args) {
        Object singletonBean = getSingleton(name);
        if (Objects.nonNull(singletonBean)) {
            return getObjectForBeanInstance(name, singletonBean);
        }
        BeanDefinition beanDefinition = getBeanDefinition(name);
        Object bean = createBean(name, beanDefinition, args);
        return getObjectForBeanInstance(name, bean);
    }

    private Object getObjectForBeanInstance(String name, Object bean) {
        if (!(bean instanceof FactoryBean)) {
            return bean;
        }
        return getObjectFromFactoryBean(name, (FactoryBean<?>) bean);
    }

    @Override
    public Object getBeanPlainly(String name, Object... args) {
        BeanDefinition beanDefinition = getBeanDefinition(name);
        Object bean = createBeanPlainly(name, beanDefinition, args);
        return getObjectForBeanInstance(name, bean);
    }

    /**
     * 获取bean定义
     *
     * @param name bean的名字
     * @return beanDefinition
     */
    public abstract BeanDefinition getBeanDefinition(String name);

    /**
     * 创建一个新的bean
     *
     * @param name           bean的名字
     * @param beanDefinition bean定义
     * @param args           构造方法的参数
     * @return bean
     */
    protected abstract Object createBean(String name, BeanDefinition beanDefinition, Object[] args);

    /**
     * 在判断目标对象不需要被代理之后，创建一个新的bean
     * 只有实例化和初始化，不包括对象作用域的处理等
     *
     * @param name           bean的名字
     * @param beanDefinition bean定义
     * @param args           构造方法的参数
     * @return bean
     */
    protected abstract Object createBeanPlainly(String name, BeanDefinition beanDefinition, Object[] args);

    @Override
    public void addBeanPostProcessor(BeanPostProcessor beanPostProcessor) {
        beanPostProcessors.add(beanPostProcessor);
    }

    @Override
    public void addEmbeddedValueResolver(StringValueResolver stringValueResolver) {
        embeddedStringValueResolvers.add(stringValueResolver);
    }

    @Override
    public String resolveEmbeddedValue(String value) {
        if (Objects.isNull(value)) {
            return null;
        }
        String result = value;
        for (StringValueResolver stringValueResolver : embeddedStringValueResolvers) {
            result = stringValueResolver.resolveStringValue(value);
            if (Objects.isNull(result)) {
                return null;
            }
        }
        return result;
    }
}
