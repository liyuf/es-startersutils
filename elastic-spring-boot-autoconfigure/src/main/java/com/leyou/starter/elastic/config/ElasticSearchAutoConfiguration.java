package com.leyou.starter.elastic.config;

import com.leyou.starter.elastic.scanner.RepositoryScanner;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.BeansException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.stream.Stream;

/**
 * @author 虎哥
 */
@Configuration
@ConditionalOnClass({Mono.class, Flux.class, RestHighLevelClient.class})
public class ElasticSearchAutoConfiguration implements ApplicationContextAware {

    // elasticsearch集群的地址，多个以','分割，默认是本机
    private String hosts = "http://127.0.0.1:9200";

    @Bean
    @ConditionalOnMissingBean
    public RestHighLevelClient restHighLevelClient() {
        return new RestHighLevelClient(
                // 利用Builder构建器来初始化，接收HttpHost数组
                RestClient.builder(
                        // 将地址以 , 分割得到其中的每个地址
                        Stream.of(StringUtils.split(hosts, ","))
                                // 将单个地址封装为HttpHost对象
                                .map(HttpHost::create)
                                // 转为HttpHost数组
                                .toArray(HttpHost[]::new)
                )
        );
    }

    @Bean
    public RepositoryScanner repositoryScanner() {
        return new RepositoryScanner(restHighLevelClient());
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        // 读取配置文件中的 "ly.elasticsearch.hosts"属性
        String hosts = applicationContext.getEnvironment().getProperty("ly.elasticsearch.hosts");
        // 覆盖默认值
        if(StringUtils.isNotBlank(hosts)){
            this.hosts = hosts;
        }
    }
}
