package com.minit.core;

import com.minit.*;
import com.minit.connector.http.HttpConnector;
import com.minit.logger.FileLogger;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class StandardContext extends ContainerBase implements Context {
    HttpConnector connector = null;
    Map<String, String> servletClsMap = new ConcurrentHashMap<>();
    Map<String, StandardWrapper> servletInstanceMap = new ConcurrentHashMap<>();

    private Map<String,ApplicationFilterConfig> filterConfigMap = new ConcurrentHashMap<>();

    private Map<String,FilterDef> filterDefs = new ConcurrentHashMap<>();

    private FilterMap filterMaps[] = new FilterMap[0];

    private ArrayList<ContainerListener> listeners = new ArrayList<>();

    private ArrayList<ContainerListenerDef> listenerDefs = new ArrayList<>();

    public void start(){
        fireContainerEvent("Container Started",this);

        Logger logger = new FileLogger();
        setLogger(logger);

        //scan web.xml
        String file = System.getProperty("minit.base") + File.separator+
                this.docbase + File.separator + "WEB_INF" + File.separator + "web.xml";

        SAXReader reader = new SAXReader();
        Document document;
        try{
            document = reader.read(file);
            Element root = document.getRootElement();

            //listener
            List<Element> listeners = root.elements("listener");
            for (Element listener : listeners) {
                Element listenerClass = listener.element("listener-class");
                String listenerClassName = listenerClass.getText();
                System.out.println("listenerClassName:"+listenerClassName);

                //load listener
                ContainerListenerDef listenerDef = new ContainerListenerDef();
                listenerDef.setListenerClass(listenerClassName);
                listenerDef.setListenerName(listenerClassName);
                addListenerDef(listenerDef);
            }

            listenerStart();
            List<Element> filters = root.elements("filter");
            for (Element filter : filters) {
                Element filetername = filter.element("filter-name");
                String fileternamestr = filetername.getText();
                Element fileterclass = filter.element("filter-class");
                String fileterclassstr = fileterclass.getText();
                System.out.println("filter " + fileternamestr + fileterclassstr);

                //load filters
                FilterDef filterDef = new FilterDef();
                filterDef.setFilterName(fileternamestr);
                filterDef.setFilterClass(fileterclassstr);
                addFilterDef(filterDef);
            }

            //filter mappings
            List<Element> filtermaps = root.elements("filter-mapping");
            for (Element filtermap : filtermaps) {
                Element filetername = filtermap.element("filter-name");
                String fileternamestr = filetername.getText();
                Element urlpattern = filtermap.element("url-pattern");
                String urlpatternstr = urlpattern.getText();
                System.out.println("filter mapping " + fileternamestr + urlpatternstr);

                FilterMap filterMap = new FilterMap();
                filterMap.setFilterName(fileternamestr);
                filterMap.setURLPattern(urlpatternstr);
                addFilterMap(filterMap);
            }

            filterStart();

            //servlet
            List<Element> servlets = root.elements("servlet");
            for (Element servlet : servlets) {
                Element servletname = servlet.element("servlet-name");
                String servletnamestr = servletname.getText();
                Element servletclass = servlet.element("servlet-class");
                String servletclassstr = servletclass.getText();
                Element loadonstartup = servlet.element("load-on-startup");
                String loadonstartupstr = null;
                if (loadonstartup != null) {
                    loadonstartupstr = loadonstartup.getText();
                }

                System.out.println("servlet " + servletnamestr + servletclassstr);

                this.servletClsMap.put(servletnamestr, servletclassstr);
                if (loadonstartupstr != null) {
                    getWrapper(servletnamestr);
                }

            }

        } catch (DocumentException e) {
            e.printStackTrace();
        }

        System.out.println("Context started.......");
    }

    public void fireContainerEvent(String type,Object data) {
        if(listeners.size() < 1)
            return;
        ContainerEvent event = new ContainerEvent(this, type, data);
        ContainerListener list[] = new ContainerListener[0];
        synchronized (listeners){
            //list会被扩大
            list = (ContainerListener[]) listeners.toArray(list);
        }
        for (int i = 0; i < list.length; i++) {
            ((ContainerListener)list[i]).containerEvent(event);
        }
    }

    public void addContainerListener(ContainerListener listener) {
        synchronized (listeners) {
            listeners.add(listener);
        }
    }
    public void removeContainerListener(ContainerListener listener) {
        synchronized (listeners) {
            listeners.remove(listener);
        }
    }

    public void addListenerDef(ContainerListenerDef listenererDef) {
        synchronized (listenerDefs) {
            listenerDefs.add(listenererDef);
        }
    }




    public StandardContext() {
        super();
        pipeline.setBasic(new StandardContextValve());
        log("Container created.");
    }

    public String getInfo() {
        return "Minit Servlet Context, vesion 0.1";
    }

    @Override
    public void addChild(Container child) {

    }

    public HttpConnector getConnector() {
        return connector;
    }

    public void setConnector(HttpConnector connector) {
        this.connector = connector;
    }


    @Override
    public void invoke(Request request, Response response) throws IOException, ServletException {
        System.out.println("StandardContext invoke()");

        super.invoke(request, response);
    }

    @Override
    public String getDisplayName() {
        return null;
    }

    @Override
    public void setDisplayName(String displayName) {

    }

    @Override
    public String getDocBase() {
        return null;
    }

    @Override
    public void setDocBase(String docBase) {

    }

    @Override
    public String getPath() {
        return null;
    }

    @Override
    public void setPath(String path) {

    }

    @Override
    public ServletContext getServletContext() {
        return null;
    }

    @Override
    public int getSessionTimeout() {
        return 0;
    }

    @Override
    public void setSessionTimeout(int timeout) {

    }

    @Override
    public String getWrapperClass() {
        return null;
    }
    public Wrapper getWrapper(String name) {
        StandardWrapper servletWrapper = servletInstanceMap.get(name);
        if (servletWrapper == null) {
            String servletClassName = name;
            servletWrapper = new StandardWrapper(servletClassName, this);
            this.servletClsMap.put(name, servletClassName);
            this.servletInstanceMap.put(name, servletWrapper);
        }
        return servletWrapper;
    }

    @Override
    public void setWrapperClass(String wrapperClass) {

    }

    @Override
    public Wrapper createWrapper() {
        return null;
    }

    @Override
    public String findServletMapping(String pattern) {
        return null;
    }

    @Override
    public String[] findServletMappings() {
        return new String[0];
    }

    @Override
    public void reload() {

    }

    public void addFilterDef(FilterDef filterDef) {
        filterDefs.put(filterDef.getFilterName(), filterDef);
    }

    public void addFilterMap(FilterMap filterMap) {
        String filterName = filterMap.getFilterName();
        String servletName = filterMap.getServletName();
        String urlPattern = filterMap.getURLPattern();
        if(findFilterDef(filterName) == null){
            throw new IllegalArgumentException("standardContext.filterMap.name"+filterName);
        }
        if ((servletName == null) && (urlPattern == null))
            throw new IllegalArgumentException("standardContext.filterMap.either");
        if ((servletName != null) && (urlPattern != null))
            throw new IllegalArgumentException("standardContext.filterMap.either");
        // Because filter-pattern is new in 2.3, no need to adjust
        // for 2.2 backwards compatibility

        if ((urlPattern != null) && !validateURLPattern(urlPattern))
            throw new IllegalArgumentException("standardContext.filterMap.pattern"+urlPattern);
        synchronized (filterMaps) {
            FilterMap results[] =new FilterMap[filterMaps.length + 1];
            System.arraycopy(filterMaps, 0, results, 0, filterMaps.length);
            results[filterMaps.length] = filterMap;
            filterMaps = results;
        }
    }

    public FilterDef findFilterDef(String filterName) {
        return ((FilterDef) filterDefs.get(filterName));
    }

    public FilterDef[] findFilterDefs(){
        synchronized (filterDefs){
            FilterDef results[] = new FilterDef[filterDefs.size()];
            return (FilterDef[]) filterDefs.values().toArray(results);
        }
    }

    public FilterMap[] findFilterMaps() {
        return filterMaps;
    }
    public void removeFilterDef(FilterDef filterDef) {
        filterDefs.remove(filterDef.getFilterName());
    }

    public void removeFilterMap(FilterMap filterMap) {
        synchronized (filterMaps) {
            int n = -1;
            for (int i = 0; i < filterMaps.length; i++) {
                if(filterMaps[i] == filterMap);
                n = i;
                break;
            }
            if(n<0)
                return;
            FilterMap results[] = new FilterMap[filterMaps.length - 1];
            System.arraycopy(filterMaps, 0, results, 0, n);
            System.arraycopy(filterMaps, n + 1, results, n, filterMaps.length - n - 1);
            filterMaps = results;
        }
    }

    public FilterConfig findFilterConfig(String name) {
        return (filterConfigMap.get(name));
    }

    public boolean filterStart(){
        // Instantiate and record a FilterConfig for each defined filter
        boolean ok = true;
        synchronized (filterConfigMap){
            filterConfigMap.clear();
            Iterator<String> names = filterDefs.keySet().iterator();
            while (names.hasNext()) {
                String name = names.next();
                ApplicationFilterConfig filterConfig = null;
                try {
                    filterConfig = new ApplicationFilterConfig(this, (FilterDef) filterDefs.get(name));
                } catch (Throwable e) {
                    ok = false;
                }
            }
        }
        return ok;
    }


    private boolean validateURLPattern(String urlPattern) {
        if (urlPattern == null)
            return (false);
        if (urlPattern.startsWith("*.")) {
            if (urlPattern.indexOf('/') < 0)
                return (true);
            else
                return (false);
        }
        if (urlPattern.startsWith("/"))
            return (true);
        else
            return (false);
    }

    public boolean listenerStart(){
        System.out.println("Listener Start..........");
        boolean ok = true;
        synchronized (listeners){
            listeners.clear();
            Iterator<ContainerListenerDef> defIterator = listenerDefs.iterator();
            while (defIterator.hasNext()){
                ContainerListenerDef listenerDef = defIterator.next();
                ContainerListener listener = null;
                try {
                    String listenerClass = listenerDef.getListenerClass();
                    Loader classLoader = null;
                    classLoader = this.getLoader();

                    ClassLoader oldCtxClassLoader =
                            Thread.currentThread().getContextClassLoader();
                    Class<?> clazz = classLoader.getClassLoader().loadClass(listenerClass);
                    listener = (ContainerListener)clazz.newInstance();
                    addContainerListener(listener);
                } catch (Throwable e) {
                    e.printStackTrace();
                    ok = false;
                }
            }
        }
        return ok;
    }

}