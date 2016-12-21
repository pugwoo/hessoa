package com.pugwoo.hessoa.annotation;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationBeanNameGenerator;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.context.annotation.ScannedGenericBeanDefinition;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.StringUtils;

import com.pugwoo.hessoa.register.SOAHessianServiceExporter;
import com.pugwoo.hessoa.utils.Constants;
import com.pugwoo.hessoa.utils.RedisUtils;

/**
 * 2015年1月6日 14:54:54
 * @author pugwoo
 * 扫描@HessianService注解的bean，它在spring容器起来之后执行
 */
public class HessianServiceScanner implements BeanFactoryPostProcessor,
    ApplicationContextAware {

	private ApplicationContext applicationContext;
	
	/**
	 * 扫描@HessianService注解的bean所在的包，支持通配符*
	 */
	private String basePackage;
	
	public void removeRedis() {
		RedisUtils.removeKey(RedisUtils.thisProcessKeys);
	}

	@Override
	public void postProcessBeanFactory(
			ConfigurableListableBeanFactory beanFactory) throws BeansException {
		Scanner scanner = new Scanner((BeanDefinitionRegistry) beanFactory);
		scanner.setResourceLoader(this.applicationContext);

		scanner.scan(StringUtils.tokenizeToStringArray(this.basePackage,
				ConfigurableApplicationContext.CONFIG_LOCATION_DELIMITERS));
	}
	
	/**
	 * AnnotationBeanNameGenerator决定了spring注解默认生成的bean名称<br>
	 * 默认的生成方式是类名首字母小写<br>
	 * com.xyz.FooServiceImpl -> fooServiceImpl  
	 */
	private final class HessianServiceBeanNameGenerator extends
			AnnotationBeanNameGenerator {

		/**
		 * 将bean名称设置为@HessianService注解的实现类的className
		 */
		protected String determineBeanNameFromAnnotation(
				AnnotatedBeanDefinition annotatedDef) {
			AnnotationMetadata amd = annotatedDef.getMetadata();
			return amd.getClassName();
		}
	}
	
	private static Object getAnnotationValue(AnnotationMetadata amd, String name) {
		for (String type : amd.getAnnotationTypes()) {
			if (type.equals(HessianService.class.getName())) {
				Map<String, Object> attributes = amd
						.getAnnotationAttributes(type);
				return attributes.get(name);
			}
		}
		return null;
	}

	/**
	 * 扫描
	 */
	private final class Scanner extends ClassPathBeanDefinitionScanner {

		private BeanNameGenerator exporterBeanNameGenerator = new HessianServiceBeanNameGenerator();
		private BeanNameGenerator beanNameGenerator = new AnnotationBeanNameGenerator();

		private BeanDefinitionRegistry registry;

		public Scanner(BeanDefinitionRegistry registry) {
			super(registry);
			this.registry = registry;
		}

		@Override
		protected Set<BeanDefinitionHolder> doScan(String... basePackages) {
			Set<BeanDefinitionHolder> beanDefinitions = new LinkedHashSet<BeanDefinitionHolder>();
			for (String basePackage : basePackages) {
				Set<BeanDefinition> candidates = findCandidateComponents(basePackage);
				for (BeanDefinition candidate : candidates) {
					String originalBeanName = this.beanNameGenerator.generateBeanName(candidate, this.registry);
					String beanName = this.exporterBeanNameGenerator.generateBeanName(candidate, this.registry);
					
					if(beanName == null || beanName.isEmpty()) {
						beanName = candidate.getBeanClassName(); // 确保beanName有值
					}
					
					// beanName必须以/开头
					if(!beanName.startsWith("/")) {
						beanName = "/" + beanName;
					}
					
					beanName = "/" + Constants.HESSOA_URL_PREFIX + beanName;
		
					ScannedGenericBeanDefinition bd = (ScannedGenericBeanDefinition) candidate;
					String[] interfaces = bd.getMetadata().getInterfaceNames();
					if (interfaces == null || interfaces.length == 0) {
						throw new RuntimeException("@HessianService bean must implement at least one interface");
					}
					Class<?> interf = null;
					Class<?> annoInterf = (Class<?>) getAnnotationValue(bd.getMetadata(), "value");
					if(annoInterf != null) {
						if(annoInterf.isInterface()) {
							interf = annoInterf;
						} else {
							throw new RuntimeException("@HessianService value must be interface class");
						}
					} else {
						throw new RuntimeException("@HessianService must have value (interface)");
					}
	
					
					/**
					 * 下面这一段，实际上等价于xml配置
					 */
					BeanDefinitionBuilder hessianBeanDef = 
							BeanDefinitionBuilder.genericBeanDefinition(SOAHessianServiceExporter.class);
					hessianBeanDef.addPropertyReference("service", originalBeanName);
					hessianBeanDef.addPropertyValue("beanName", beanName);
					hessianBeanDef.addPropertyValue("serviceInterface", interf.getName());

					BeanDefinitionHolder definitionHolder = new BeanDefinitionHolder(
							hessianBeanDef.getBeanDefinition(), beanName);
					beanDefinitions.add(definitionHolder);
					registerBeanDefinition(definitionHolder, this.registry);
				}
			}
			
			// 打印扫描信息，可用于debug
			if (beanDefinitions.isEmpty()) {
				System.out.println("no service scaned");
			} else {
				for (BeanDefinitionHolder holder : beanDefinitions) {
					System.out.println("scanned service:" + holder.getBeanName());
				}
			}

			return beanDefinitions;
		}

		@Override
		protected void registerDefaultFilters() {
			addIncludeFilter(new AnnotationTypeFilter(HessianService.class));
		}
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext)
			throws BeansException {
		this.applicationContext = applicationContext;

	}

	public String getBasePackage() {
		return basePackage;
	}

	public void setBasePackage(String basePackage) {
		this.basePackage = basePackage;
	}

}
