package org.apache.syncope.core.spring.security;

import org.junit.Assert;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.*;
import org.springframework.core.ResolvableType;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.io.ProtocolResolver;
import org.springframework.core.io.Resource;
import org.springframework.core.metrics.ApplicationStartup;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class DummyConfigurableApplicationContext implements ConfigurableApplicationContext {
    private DefaultListableBeanFactory beanFactory;

    public DummyConfigurableApplicationContext(DefaultListableBeanFactory beanFactory){
        this.beanFactory=beanFactory;
    }
    @Override
    public void setId(String id) {

    }

    @Override
    public void setParent(ApplicationContext parent) {

    }

    @Override
    public void setEnvironment(ConfigurableEnvironment environment) {

    }

    @Override
    public ConfigurableEnvironment getEnvironment() {
        return null;
    }

    @Override
    public void setApplicationStartup(ApplicationStartup applicationStartup) {

    }

    @Override
    public ApplicationStartup getApplicationStartup() {
        return null;
    }

    @Override
    public void addBeanFactoryPostProcessor(BeanFactoryPostProcessor postProcessor) {

    }

    @Override
    public void addApplicationListener(ApplicationListener<?> listener) {

    }

    @Override
    public void removeApplicationListener(ApplicationListener<?> listener) {

    }

    @Override
    public void setClassLoader(ClassLoader classLoader) {

    }

    @Override
    public void addProtocolResolver(ProtocolResolver resolver) {

    }

    @Override
    public void refresh() throws BeansException, IllegalStateException {

    }

    @Override
    public void registerShutdownHook() {

    }

    @Override
    public void close() {

    }

    @Override
    public boolean isActive() {
        return false;
    }

    @Override
    public ConfigurableListableBeanFactory getBeanFactory() throws IllegalStateException {
        return null;
    }

    @Override
    public String getId() {
        return null;
    }

    @Override
    public String getApplicationName() {
        return null;
    }

    @Override
    public String getDisplayName() {
        return null;
    }

    @Override
    public long getStartupDate() {
        return 0;
    }

    @Override
    public ApplicationContext getParent() {
        return null;
    }

    @Override
    public AutowireCapableBeanFactory getAutowireCapableBeanFactory() throws IllegalStateException {
        return null;
    }

    @Override
    public BeanFactory getParentBeanFactory() {
        return null;
    }

    @Override
    public boolean containsLocalBean(String s) {
        return false;
    }

    @Override
    public boolean containsBeanDefinition(String s) {
        return false;
    }

    @Override
    public int getBeanDefinitionCount() {
        return 0;
    }

    @Override
    public String[] getBeanDefinitionNames() {
        return new String[0];
    }

    @Override
    public <T> ObjectProvider<T> getBeanProvider(Class<T> aClass, boolean b) {
        return null;
    }

    @Override
    public <T> ObjectProvider<T> getBeanProvider(ResolvableType resolvableType, boolean b) {
        return null;
    }

    @Override
    public String[] getBeanNamesForType(ResolvableType resolvableType) {
        return new String[0];
    }

    @Override
    public String[] getBeanNamesForType(ResolvableType resolvableType, boolean b, boolean b1) {
        return new String[0];
    }

    @Override
    public String[] getBeanNamesForType(Class<?> aClass) {
        return new String[0];
    }

    @Override
    public String[] getBeanNamesForType(Class<?> aClass, boolean b, boolean b1) {
        return new String[0];
    }

    @Override
    public <T> Map<String, T> getBeansOfType(Class<T> aClass) throws BeansException {
        return null;
    }

    @Override
    public <T> Map<String, T> getBeansOfType(Class<T> aClass, boolean b, boolean b1) throws BeansException {
        return null;
    }

    @Override
    public String[] getBeanNamesForAnnotation(Class<? extends Annotation> aClass) {
        return new String[0];
    }

    @Override
    public Map<String, Object> getBeansWithAnnotation(Class<? extends Annotation> aClass) throws BeansException {
        return null;
    }

    @Override
    public <A extends Annotation> A findAnnotationOnBean(String s, Class<A> aClass) throws NoSuchBeanDefinitionException {
        return null;
    }

    @Override
    public <A extends Annotation> A findAnnotationOnBean(String s, Class<A> aClass, boolean b) throws NoSuchBeanDefinitionException {
        return null;
    }

    @Override
    public <A extends Annotation> Set<A> findAllAnnotationsOnBean(String beanName, Class<A> annotationType, boolean allowFactoryBeanInit) throws NoSuchBeanDefinitionException {
        return null;
    }

    @Override
    public Object getBean(String s) throws BeansException {
        return beanFactory.getBean("securityProperties");
    }

    @Override
    public <T> T getBean(String s, Class<T> aClass) throws BeansException {
        return null;
    }

    @Override
    public Object getBean(String s, Object... objects) throws BeansException {
        return null;
    }

    @Override
    public <T> T getBean(Class<T> aClass) throws BeansException {
        return (T) beanFactory.getBean("securityProperties");
    }

    @Override
    public <T> T getBean(Class<T> aClass, Object... objects) throws BeansException {
        return null;
    }

    @Override
    public <T> ObjectProvider<T> getBeanProvider(Class<T> aClass) {
        return null;
    }

    @Override
    public <T> ObjectProvider<T> getBeanProvider(ResolvableType resolvableType) {
        return null;
    }

    @Override
    public boolean containsBean(String s) {
        return false;
    }

    @Override
    public boolean isSingleton(String s) throws NoSuchBeanDefinitionException {
        return false;
    }

    @Override
    public boolean isPrototype(String s) throws NoSuchBeanDefinitionException {
        return false;
    }

    @Override
    public boolean isTypeMatch(String s, ResolvableType resolvableType) throws NoSuchBeanDefinitionException {
        return false;
    }

    @Override
    public boolean isTypeMatch(String s, Class<?> aClass) throws NoSuchBeanDefinitionException {
        return false;
    }

    @Override
    public Class<?> getType(String s) throws NoSuchBeanDefinitionException {
        return null;
    }

    @Override
    public Class<?> getType(String s, boolean b) throws NoSuchBeanDefinitionException {
        return null;
    }

    @Override
    public String[] getAliases(String s) {
        return new String[0];
    }

    @Override
    public void publishEvent(Object event) {

    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {

    }

    @Override
    public boolean isRunning() {
        return false;
    }

    @Override
    public String getMessage(String code, Object[] args, String defaultMessage, Locale locale) {
        return null;
    }

    @Override
    public String getMessage(String code, Object[] args, Locale locale) throws NoSuchMessageException {
        return null;
    }

    @Override
    public String getMessage(MessageSourceResolvable resolvable, Locale locale) throws NoSuchMessageException {
        return null;
    }

    @Override
    public Resource[] getResources(String locationPattern) throws IOException {
        return new Resource[0];
    }

    @Override
    public Resource getResource(String location) {
        return null;
    }

    @Override
    public ClassLoader getClassLoader() {
        return null;
    }
}
