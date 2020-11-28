package com.lagou.edu.factory;

import com.alibaba.druid.util.StringUtils;
import com.lagou.edu.annotation.Autowired;
import com.lagou.edu.annotation.Service;
import com.lagou.edu.annotation.Transactional;
import org.reflections.Reflections;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author 应癫
 * <p>
 * 工厂类，生产对象（使用反射技术）
 */
public class BeanFactory {

    /**
     * 任务一：读取解析xml，通过反射技术实例化对象并且存储待用（map集合）
     * 任务二：对外提供获取实例对象的接口（根据id获取）
     */

    private static Map<String, Object> map = new HashMap<>();  // 存储对象


    static {
        try {
            //扫描包，获取需要实例化的对象列表，通过反射实例化
            //获取反射对象集合
            Reflections reflections = new Reflections("com.lagou.edu");
            //获取有service注解的类
            Set<Class<?>> set = reflections.getTypesAnnotatedWith(Service.class);
            for (Class<?> aClass : set) {
                Object bean = aClass.newInstance();
                Service annotation = aClass.getAnnotation(Service.class);
                //根据Service注解中是否有Value值来处理
                if (StringUtils.isEmpty(annotation.value())) {
                    //去掉全限定名的包名，取类名
                    String[] names = aClass.getName().split("\\.");
                    map.put(names[names.length - 1], bean);
                } else {
                    map.put(annotation.value(), bean);
                }
            }

            for (Map.Entry<String, Object> a : map.entrySet()) {
                //依赖注入
                Object o = a.getValue();
                Class c = o.getClass();
                Field[] fields = c.getDeclaredFields();
                for (Field field : fields) {
                    //对类中有Autowired注解的类成员调用set方法
                    if (field.isAnnotationPresent(Autowired.class)
                            && field.getAnnotation(Autowired.class).required()) {
                        String[] names = field.getType().getName().split("\\.");
                        String name = names[names.length - 1];
                        Method[] methods = c.getMethods();
                        for (int j = 0; j < methods.length; j++) {
                            Method method = methods[j];
                            if (method.getName().equalsIgnoreCase("set" + name)) {
                                method.invoke(o, map.get(name));
                            }

                        }
                    }
                }
            }
            //获取代理对象，进行事务控制
            for (Map.Entry<String, Object> a : map.entrySet()) {
                Object o = a.getValue();
                Class c = o.getClass();
                if (c.isAnnotationPresent(Transactional.class)) {
                    ProxyFactory proxyFactory = (ProxyFactory) BeanFactory.getBean("proxyFactory");
                    Class[] face = c.getInterfaces();
                    if (face != null && face.length > 0) {
                        o = proxyFactory.getJdkProxy(o);
                    } else {
                        o = proxyFactory.getCglibProxy(o);
                    }
                }
                map.put(a.getKey(), o);
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }


    /*static {
        // 任务一：读取解析xml，通过反射技术实例化对象并且存储待用（map集合）
        // 加载xml
        InputStream resourceAsStream = BeanFactory.class.getClassLoader().getResourceAsStream("beans.xml");
        // 解析xml
        SAXReader saxReader = new SAXReader();
        try {
            Document document = saxReader.read(resourceAsStream);
            Element rootElement = document.getRootElement();
            List<Element> beanList = rootElement.selectNodes("//bean");
            for (int i = 0; i < beanList.size(); i++) {
                Element element =  beanList.get(i);
                // 处理每个bean元素，获取到该元素的id 和 class 属性
                String id = element.attributeValue("id");        // accountDao
                String clazz = element.attributeValue("class");  // com.lagou.edu.dao.impl.JdbcAccountDaoImpl
                // 通过反射技术实例化对象
                Class<?> aClass = Class.forName(clazz);
                Object o = aClass.newInstance();  // 实例化之后的对象

                // 存储到map中待用
                map.put(id,o);

            }

            // 实例化完成之后维护对象的依赖关系，检查哪些对象需要传值进入，根据它的配置，我们传入相应的值
            // 有property子元素的bean就有传值需求
            List<Element> propertyList = rootElement.selectNodes("//property");
            // 解析property，获取父元素
            for (int i = 0; i < propertyList.size(); i++) {
                Element element =  propertyList.get(i);   //<property name="AccountDao" ref="accountDao"></property>
                String name = element.attributeValue("name");
                String ref = element.attributeValue("ref");

                // 找到当前需要被处理依赖关系的bean
                Element parent = element.getParent();

                // 调用父元素对象的反射功能
                String parentId = parent.attributeValue("id");
                Object parentObject = map.get(parentId);
                // 遍历父对象中的所有方法，找到"set" + name
                Method[] methods = parentObject.getClass().getMethods();
                for (int j = 0; j < methods.length; j++) {
                    Method method = methods[j];
                    if(method.getName().equalsIgnoreCase("set" + name)) {  // 该方法就是 setAccountDao(AccountDao accountDao)
                        method.invoke(parentObject,map.get(ref));
                    }
                }

                // 把处理之后的parentObject重新放到map中
                map.put(parentId,parentObject);

            }


        } catch (DocumentException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }

    }*/


    // 任务二：对外提供获取实例对象的接口（根据id获取）
    public static Object getBean(String id) {
        return map.get(id);
    }

}
