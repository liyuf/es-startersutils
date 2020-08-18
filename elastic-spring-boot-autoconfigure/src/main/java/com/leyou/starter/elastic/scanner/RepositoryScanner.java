package com.leyou.starter.elastic.scanner;

import com.leyou.starter.elastic.repository.Repository;
import com.leyou.starter.elastic.repository.RepositoryFactory;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternUtils;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.util.ClassUtils;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * @author 虎哥
 */
public class RepositoryScanner implements BeanDefinitionRegistryPostProcessor, ResourceLoaderAware, ApplicationContextAware {
    /**
     * spring容器
     */
    private ApplicationContext applicationContext;
    /**
     * 扫描的文件类型
     */
    private static final String DEFAULT_RESOURCE_PATTERN = "**/*.class";

    private MetadataReaderFactory metadataReaderFactory;
    /**
     * 资源解析器，读取classpath下的内容
     */
    private ResourcePatternResolver resourcePatternResolver;
    /**
     * elasticsearch客户端
     */
    private RestHighLevelClient client;

    public RepositoryScanner(RestHighLevelClient client) {
        this.client = client;
    }

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry beanDefinitionRegistry) throws BeansException {
        // 获取启动类所在包
        List<String> packages = AutoConfigurationPackages.get(applicationContext);
        // 开始扫描包，获取字节码
        Set<Class<?>> beanClazzSet = scannerPackages(packages.get(0));
        for (Class beanClazz : beanClazzSet) {
            // 判断是否是repository
            if (isNotElasticsearchRepository(beanClazz)) {
                continue;
            }
            // BeanDefinition构建器
            BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(beanClazz);
            GenericBeanDefinition definition = (GenericBeanDefinition) builder.getRawBeanDefinition();

            // 设置RepositoryFactory的构造函数参数
            definition.getConstructorArgumentValues().addGenericArgumentValue(beanClazz);
            definition.getConstructorArgumentValues().addIndexedArgumentValue(1, client);
            // 设置FactoryBean工程
            definition.setBeanClass(RepositoryFactory.class);

            // 这里采用的是AutoWired的byType方式注入，类似的还有byName等
            definition.setAutowireMode(GenericBeanDefinition.AUTOWIRE_BY_TYPE);
            // 生成默认的Bean名称，是类名称首字母小写
            String simpleName = beanClazz.getSimpleName();
            simpleName = simpleName.substring(0, 1).toLowerCase() + simpleName.substring(1);
            // 注册bean
            beanDefinitionRegistry.registerBeanDefinition(simpleName, definition);
        }
    }

    private boolean isNotElasticsearchRepository(Class beanClazz) {
        return !beanClazz.isInterface() || beanClazz.getInterfaces().length <= 0 || beanClazz.getInterfaces()[0] != Repository.class;
    }

    /**
     * 根据包路径获取包及子包下的所有类
     *
     * @param basePackage basePackage
     * @return Set<Class   <   ?>> Set<Class<?>>
     */
    private Set<Class<?>> scannerPackages(String basePackage) {
        // 准备集合，装扫描到的类
        Set<Class<?>> set = new LinkedHashSet<>();
        // 设置要扫描的文件路径匹配模板 classpath*:/xx/xx/**/*.class
        String packageSearchPath =
                // classpath*:
                ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX +
                        // 启动类所在包
                        resolveBasePackage(basePackage) +
                        // **/*.class
                        '/' + DEFAULT_RESOURCE_PATTERN;
        try {
            // 读取符合匹配模板的所有文件
            Resource[] resources = this.resourcePatternResolver.getResources(packageSearchPath);
            for (Resource resource : resources) {
                if (resource.isReadable()) {
                    MetadataReader metadataReader = this.metadataReaderFactory.getMetadataReader(resource);
                    // 读取类名称
                    String className = metadataReader.getClassMetadata().getClassName();
                    Class<?> clazz;
                    try {
                        // 加载为字节码
                        clazz = Class.forName(className);
                        set.add(clazz);
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return set;
    }

    private String resolveBasePackage(String basePackage) {
        return ClassUtils.convertClassNameToResourcePath(
                this.applicationContext.getEnvironment().resolveRequiredPlaceholders(basePackage)
        );
    }


    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourcePatternResolver = ResourcePatternUtils.getResourcePatternResolver(resourceLoader);
        this.metadataReaderFactory = new CachingMetadataReaderFactory(resourceLoader);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }


    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory configurableListableBeanFactory) throws BeansException {

    }
}
