package minispring.util;

/**
 * @author lihua
 * @since 2021/8/27
 */
public class ClassUtils {

	private ClassUtils() {
		throw new UnsupportedOperationException("Utility class!");
	}

	public static final String CGLIB_CLASS_SEPARATOR = "$$";

	/**
	 * 直接从spring framework拷过来的，获取默认的类加载器
	 *
	 * @return classLoader
	 */
	public static ClassLoader getDefaultClassLoader() {
		ClassLoader cl = null;
		try {
			cl = Thread.currentThread().getContextClassLoader();
		} catch (Exception ex) {
			// Cannot access thread context ClassLoader - falling back...
		}
		if (cl == null) {
			// No thread context class loader -> use class loader of this class.
			cl = ClassUtils.class.getClassLoader();
			if (cl == null) {
				// getClassLoader() returning null indicates the bootstrap ClassLoader
				try {
					cl = ClassLoader.getSystemClassLoader();
				} catch (Exception ex) {
					// Cannot access system ClassLoader - oh well, maybe the caller can live with null...
				}
			}
		}
		return cl;
	}

	/**
	 * Check whether the specified class is a CGLIB-generated class.
	 *
	 * @param clazz the class to check
	 * @return 判断结果
	 */
	public static boolean isCglibProxyClass(Class<?> clazz) {
		return (clazz != null && isCglibProxyClassName(clazz.getName()));
	}

	/**
	 * Check whether the specified class name is a CGLIB-generated class.
	 *
	 * @param className the class name to check
	 * @return 判断结果
	 */
	public static boolean isCglibProxyClassName(String className) {
		return (className != null && className.contains(CGLIB_CLASS_SEPARATOR));
	}

	/**
	 * 获取对象的真正类对象
	 * 如果对象是CGLIB代理而来的，则是继承了类的；
	 * 否则，JDK代理或没有代理的对象，都是实现接口的。
	 *
	 * @param object 对象
	 * @return 类对象
	 */
	public static Class<?> getRealClass(Object object) {
		Class<?> originalClass = object.getClass();
		return ClassUtils.isCglibProxyClass(originalClass) ? originalClass.getSuperclass() : originalClass;
	}
}
