/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.apache.isis.viewer.scimpi.dispatcher;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.TimeZone;

import org.apache.isis.applib.Identifier;
import org.apache.isis.core.commons.authentication.AuthenticationSession;
import org.apache.isis.core.commons.config.ConfigurationConstants;
import org.apache.isis.core.commons.debug.DebugBuilder;
import org.apache.isis.core.commons.debug.Debuggable;
import org.apache.isis.core.commons.exceptions.IsisException;
import org.apache.isis.core.commons.factory.InstanceUtil;
import org.apache.isis.core.metamodel.adapter.ObjectAdapter;
import org.apache.isis.core.metamodel.facetapi.Facet;
import org.apache.isis.core.metamodel.facets.collections.modify.CollectionFacet;
import org.apache.isis.core.metamodel.facets.typeof.TypeOfFacet;
import org.apache.isis.core.metamodel.spec.ObjectSpecification;
import org.apache.isis.core.runtime.system.context.IsisContext;
import org.apache.isis.core.runtime.system.persistence.PersistenceSession;
import org.apache.isis.core.runtime.system.transaction.IsisTransactionManager;
import org.apache.isis.core.runtime.userprofile.UserLocalization;
import org.apache.isis.viewer.scimpi.DebugHtmlWriter;
import org.apache.isis.viewer.scimpi.ForbiddenException;
import org.apache.isis.viewer.scimpi.Names;
import org.apache.isis.viewer.scimpi.NotLoggedInException;
import org.apache.isis.viewer.scimpi.ScimpiContext;
import org.apache.isis.viewer.scimpi.ScimpiException;
import org.apache.isis.viewer.scimpi.dispatcher.action.ActionAction;
import org.apache.isis.viewer.scimpi.dispatcher.action.DebugAction;
import org.apache.isis.viewer.scimpi.dispatcher.action.DebugUserAction;
import org.apache.isis.viewer.scimpi.dispatcher.action.EditAction;
import org.apache.isis.viewer.scimpi.dispatcher.action.LogAction;
import org.apache.isis.viewer.scimpi.dispatcher.action.LogonAction;
import org.apache.isis.viewer.scimpi.dispatcher.action.LogoutAction;
import org.apache.isis.viewer.scimpi.dispatcher.action.RemoveAction;
import org.apache.isis.viewer.scimpi.dispatcher.context.Action;
import org.apache.isis.viewer.scimpi.dispatcher.context.DebugUsers;
import org.apache.isis.viewer.scimpi.dispatcher.context.ErrorCollator;
import org.apache.isis.viewer.scimpi.dispatcher.context.Request;
import org.apache.isis.viewer.scimpi.dispatcher.context.Response;
import org.apache.isis.viewer.scimpi.dispatcher.context.ScimpiNotFoundException;
import org.apache.isis.viewer.scimpi.dispatcher.context.Request.Debug;
import org.apache.isis.viewer.scimpi.dispatcher.context.Request.Scope;
import org.apache.isis.viewer.scimpi.dispatcher.processor.ElementProcessor;
import org.apache.isis.viewer.scimpi.dispatcher.processor.HtmlFileParser;
import org.apache.isis.viewer.scimpi.dispatcher.processor.ElementProcessorLookup;
import org.apache.isis.viewer.scimpi.dispatcher.processor.Snippet;
import org.apache.isis.viewer.scimpi.dispatcher.processor.TemplateProcessingException;
import org.apache.isis.viewer.scimpi.dispatcher.processor.TemplateProcessor;
import org.apache.isis.viewer.scimpi.dispatcher.util.MethodsUtils;
import org.apache.isis.viewer.scimpi.dispatcher.util.UserManager;
import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

public class Dispatcher implements Debuggable {
    private static final String SHOW_UNSHOWN_MESSAGES = ConfigurationConstants.ROOT + "scimpi.show-unshown-messages";
    @Deprecated
    public static final String ACTION = "_action";
    @Deprecated
    public static final String EDIT = "_edit";
    @Deprecated
    public static final String REMOVE = "_remove";
    @Deprecated
    public static final String GENERIC = "_generic";
    @Deprecated
    public static final String EXTENSION = "shtml";
    private static final Logger LOG = Logger.getLogger(Dispatcher.class);
    @Deprecated
    public static final String COMMAND_ROOT = ".app";
    private final Map<String, Action> actions = new HashMap<String, Action>();
    private final Map<String, String> parameters = new HashMap<String, String>();
    private final ElementProcessorLookup processors;
    private final HtmlFileParser parser;
    private boolean showUnshownMessages;

    public Dispatcher() {
        ScimpiContext context = new RuntimeScimpiContext();
        processors = new ElementProcessorLookup(context);
        parser = new HtmlFileParser(processors);
    }
    
    public void process(final Request request, final Response response, final String servletPath) {
        LOG.debug("processing request " + servletPath);
        final AuthenticationSession session = UserManager.startRequest(request.getSession());
        LOG.debug("  using session: " + session);
        
        String language = (String) request.getVariable("user-language");
        if (language != null) {
            Locale locale = LocaleUtil.locale(language);
            TimeZone timeZone = LocaleUtil.timeZone((String) request.getVariable("user-time-zone"));
            IsisContext.getUserProfile().setLocalization(new UserLocalization(locale, timeZone));
         } 
        
        IsisContext.getPersistenceSession().getTransactionManager().startTransaction();
        request.setRequestPath(servletPath);
        request.startRequest();

        try {
            processActions(request, false, servletPath);
            processTheView(request);
        } catch (final ScimpiNotFoundException e) {
            if (request.isInternalRequest()) {
                LOG.error("invalid page request (from within application): " + e.getMessage());
                ErrorCollator error = new ErrorCollator(); 
                error.missingFile("Failed to find page " + servletPath + "."); 
                show500ErrorPage(request, e, error);             
            } else {
                LOG.info("invalid page request (from outside application): " + e.getMessage());
                show404ErrorPage(request, servletPath); 
            }
        } catch (final NotLoggedInException e) {
            redirectToLoginPage(request); 
        } catch (final Throwable e) {
            ErrorCollator error = new ErrorCollator();
            final PersistenceSession checkSession = IsisContext.getPersistenceSession();
            final IsisTransactionManager transactionManager = checkSession.getTransactionManager();
            if (transactionManager.getTransaction() != null && transactionManager.getTransaction().getState().canAbort()) {
                transactionManager.abortTransaction();
                transactionManager.startTransaction();
            }

            final Throwable ex = e instanceof TemplateProcessingException ? e.getCause() : e;
            if (ex instanceof ForbiddenException) {
                LOG.error("invalid access to " + servletPath, e);
                show403ErrorPage(request, error, e, ex);
            } else {
                LOG.error("error procesing " + servletPath, e);
                if (request.getErrorMessage() != null) {
                    fallbackToSimpleErrorPage(request, e);
                } else {
                    show500ErrorPage(request, e, error);
                }
            }
        } finally {
            try {
                UserManager.endRequest(request.getSession());
            } catch (final Exception e1) {
                LOG.error("endRequest call failed", e1);
            }
        }
    }

    private void redirectToLoginPage(final Request context) {
        IsisContext.getMessageBroker().addWarning(
            "You are not currently logged in! Please log in so you can continue.");
        context.setRequestPath("/login.shtml");
        try {
            processTheView(context);
        } catch (final IOException e1) {
            throw new ScimpiException(e1);
        }
    }

    private void show404ErrorPage(final Request context, final String servletPath) {
        ErrorCollator error = new ErrorCollator();
        error.missingFile("Failed to find page " + servletPath + ".");
        context.raiseError(404, error);
    }

    private void show403ErrorPage(final Request context, ErrorCollator error, final Throwable e, final Throwable ex) {
        DebugBuilder debug = error.getDebug();
        error.message(e);
        error.message(ex);
        
        final List<String> roles = ((ForbiddenException) ex).getRoles();
        final StringBuffer roleList = new StringBuffer();
        for (final String role : roles) {
            if (roleList.length() > 0) {
                roleList.append("|");
            }
            roleList.append(role);
        }
        final Identifier identifier =  ((ForbiddenException) ex).getIdentifier(); 
        if (identifier != null) {
            debug.appendln("Class", identifier.toClassIdentityString() + ":" + roleList);
            debug.appendln("Member",identifier.toClassAndNameIdentityString() + ":" + roleList); 
            debug.appendln("Other",identifier.toFullIdentityString() + ":" + roleList); 
        }
        
        error.compileError(context);
        context.raiseError(403, error);
    }

    private void show500ErrorPage(final Request context, final Throwable e, ErrorCollator error) {
        error.exception(e);
        error.compileError(context);
        context.raiseError(500, error);
    }

    private void fallbackToSimpleErrorPage(final Request context, final Throwable e) {
        context.setContentType("text/html");
        final PrintWriter writer = context.getWriter();
        writer.write("<html><head><title>Error</title></head>");
        writer.write("<body><h1>Error</h1>");
        writer.write("<p>Error while processing error</p><pre>");
        e.printStackTrace(writer);
        writer.write("</pre></body></html>");
        writer.close();
        LOG.error("Error while processing error", e);
    }

    protected void processTheView(final Request context) throws IOException {
        IsisTransactionManager transactionManager = IsisContext.getPersistenceSession().getTransactionManager();
        if (transactionManager.getTransaction().getState().canFlush()) {
            transactionManager.flushTransaction();
        }
        processView(context);
        // Note - the session will have changed since the earlier call if a user
        // has logged in or out in the action
        // processing above.
        transactionManager = IsisContext.getPersistenceSession().getTransactionManager();
        if (transactionManager.getTransaction().getState().canCommit()) {
            IsisContext.getPersistenceSession().getTransactionManager().endTransaction();
        }

        context.endRequest();
        UserManager.endRequest(context.getSession());
    }

    public void addParameter(final String name, final String value) {
        parameters.put(name, value);
    }

    private String getParameter(final String name) {
        return parameters.get(name);
    }

    private void processActions(final Request context, final boolean userLoggedIn, final String actionName) throws IOException {
        if (actionName.endsWith(COMMAND_ROOT)) {
            final int pos = actionName.lastIndexOf('/');
            final Action action = actions.get(actionName.substring(pos, actionName.length() - COMMAND_ROOT.length()));
            if (action == null) {
                throw new ScimpiException("No logic for " + actionName);
            }

            LOG.debug("processing action: " + action);
            action.process(context);
            final String fowardTo = context.forwardTo();
            if (fowardTo != null) {
                processActions(context, true, fowardTo);
            }
        }
    }

    private void processView(final Request context) throws IOException {
        String file = context.getRequestedFile();
        if (file == null) {
            LOG.warn("No view specified to process");
            return;
        }
        if (file.endsWith(COMMAND_ROOT)) {
            return;
        }
        file = determineFile(context, file);
        final String fullPath = context.requestedFilePath(file);
        LOG.debug("processing file " + fullPath);
        context.setResourcePath(fullPath);

        context.setContentType("text/html");

        context.addVariable("title", "Untitled Page", Scope.REQUEST);
        final Stack<Snippet> tags = loadPageTemplate(context, fullPath);
        final TemplateProcessor templateProcessor = new TemplateProcessor(file, context, context, tags, processors);
        templateProcessor.appendDebug("processing " + fullPath);
        try {
            templateProcessor.processNextTag();
            noteIfMessagesHaveNotBeenDisplay(context);
            IsisContext.getUpdateNotifier().clear();
        } catch (final RuntimeException e) {
            IsisContext.getMessageBroker().getMessages();
            IsisContext.getMessageBroker().getWarnings();
            IsisContext.getUpdateNotifier().clear();
            throw e;
        }
        final String page = templateProcessor.popBuffer();
        final PrintWriter writer = context.getWriter();
        writer.write(page);
        if (context.getDebug() == Debug.PAGE) {
            final DebugHtmlWriter view = new DebugHtmlWriter(writer, false);
            context.append(view);
        }
    }

    public void noteIfMessagesHaveNotBeenDisplay(final Request context) {
        final List<String> messages = IsisContext.getMessageBroker().getMessages();
        if (showUnshownMessages) {
            if (messages.size() > 0) {
                context.getWriter().println("<ol class=\"messages forced\">");
                for (String message : messages) {
                    context.getWriter().println("<li>" + message + "</li>");                
                }
                context.getWriter().println("</ol>");
            }
            final List<String> warnings = IsisContext.getMessageBroker().getWarnings();
            if (warnings.size() > 0) {
                context.getWriter().println("<ol class=\"warnings forced\">");
                for (String message : warnings) {
                    context.getWriter().println("<li>" + message + "</li>");                
                }
                context.getWriter().println("</ol>");
            }
        }
    }

    private String determineFile(final Request context, String file) {
        final String fileName = file.trim();
        if (fileName.startsWith(Names.GENERIC)) {
            final Object result = context.getVariable(Names.RESULT);
            final ObjectAdapter mappedObject = MethodsUtils.findObject(context, (String) result);
            if (mappedObject == null) {
                throw new ScimpiException("No object mapping for " + result);
            }
            if (fileName.equals(Names.GENERIC + "." + Names.EXTENSION)) {
                final Facet facet = mappedObject.getSpecification().getFacet(CollectionFacet.class);
                if (facet != null) {
                    final ObjectSpecification specification = mappedObject.getSpecification();
                    final TypeOfFacet facet2 = specification.getFacet(TypeOfFacet.class);
                    file = findFileForSpecification(context, facet2.valueSpec(), "collection", Names.EXTENSION);
                } else {
                    final ObjectAdapter mappedObject2 = mappedObject;
                    if (mappedObject2.isTransient()) {
                        file = findFileForSpecification(context, mappedObject.getSpecification(), "edit", Names.EXTENSION);
                    } else {
                        file = findFileForSpecification(context, mappedObject.getSpecification(), "object", Names.EXTENSION);
                    }
                }
            } else if (fileName.equals(Names.GENERIC + Names.EDIT + "." + Names.EXTENSION)) {
                file = findFileForSpecification(context, mappedObject.getSpecification(), "edit", Names.EXTENSION);
            } else if (fileName.equals(Names.GENERIC + Names.ACTION + "." + Names.EXTENSION)) {
                final String method = context.getParameter("method");
                file = findFileForSpecification(context, mappedObject.getSpecification(), method, "action", Names.EXTENSION);
            }
        }
        return file;
    }

    private String findFileForSpecification(final Request context, final ObjectSpecification specification, final String name, final String extension) {
        return findFileForSpecification(context, specification, name, name, extension);
    }

    private String findFileForSpecification(final Request context, final ObjectSpecification specification, final String name, final String defaultName, final String extension) {

        String find = findFile(context, specification, name, extension);
        if (find == null) {
            find = "/generic/" + defaultName + "." + extension;
        }
        return find;
    }

    private String findFile(final Request context, final ObjectSpecification specification, final String name, final String extension) {
        final String className = specification.getShortIdentifier();
        String fileName = context.findFile("/" + className + "/" + name + "." + extension);
        if (fileName == null) {
            final List<ObjectSpecification> interfaces = specification.interfaces();
            for (int i = 0; i < interfaces.size(); i++) {
                fileName = findFile(context, interfaces.get(i), name, extension);
                if (fileName != null) {
                    return fileName;
                }
            }
            if (specification.superclass() != null) {
                fileName = findFile(context, specification.superclass(), name, extension);
            }
        }
        return fileName;
    }

    private Stack<Snippet> loadPageTemplate(final Request context, final String path) throws IOException, FileNotFoundException {
        // TODO cache stacks and check for them first
        copyParametersToVariableList(context);
        LOG.debug("parsing source " + path);
        return parser.parseHtmlFile(path, context);
    }

    private void copyParametersToVariableList(final Request context) {
        /*
         * Enumeration parameterNames = context.getParameterNames(); while
         * (parameterNames.hasMoreElements()) { String name = (String)
         * parameterNames.nextElement(); if (!name.equals("view")) {
         * context.addVariable(name, context.getParameter(name), Scope.REQUEST);
         * } }
         */
    }

    public void init(final String dir, final DebugUsers debugUsers) {
        addAction(new ActionAction());

        // TODO remove
        addAction(new DebugAction(this));
        addAction(new DebugUserAction(debugUsers));
        addAction(new EditAction());
        addAction(new RemoveAction());
        addAction(new LogonAction());
        addAction(new LogoutAction());
        addAction(new LogAction());

        final String configFile = getParameter("config");
        if (configFile != null) {
            final File file = new File(dir, configFile);
            if (file.exists()) {
                loadConfigFile(file);
            } else {
                throw new ScimpiException("Configuration file not found: " + configFile);
            }
        }

        PreinstallElementProcessors.init(processors);
        processors.addElementProcessor(new org.apache.isis.viewer.scimpi.dispatcher.view.debug.Debug(this));
        
        showUnshownMessages = IsisContext.getConfiguration().getBoolean(SHOW_UNSHOWN_MESSAGES, true);
    }

    private void loadConfigFile(final File file) {
        try {
            Document document;
            final SAXReader reader = new SAXReader();
            document = reader.read(file);
            final Element root = document.getRootElement();
            for (final Iterator i = root.elementIterator(); i.hasNext();) {
                final Element element = (Element) i.next();

                if (element.getName().equals("actions")) {
                    for (final Iterator actions = element.elementIterator("action"); actions.hasNext();) {
                        final Element action = (Element) actions.next();
                        final String className = action.getText();
                        final Action instance = (Action) InstanceUtil.createInstance(className);
                        addAction(instance);
                    }
                }

                if (element.getName().equals("processors")) {
                    for (final Iterator processors = element.elementIterator("processor"); processors.hasNext();) {
                        final Element processor = (Element) processors.next();
                        final String className = processor.getText();
                        final ElementProcessor instance = (ElementProcessor) InstanceUtil.createInstance(className);
                        this.processors.addElementProcessor(instance);
                    }
                }

            }
        } catch (final DocumentException e) {
            throw new IsisException(e);
        }

    }

    private void addAction(final Action action) {
        actions.put("/" + action.getName(), action);
        action.init();
    }

    public void debugData(final DebugBuilder debug) {
        debug.startSection("Actions");
        final Set<String> keySet = actions.keySet();
        final ArrayList<String> list = new ArrayList<String>(keySet);
        Collections.sort(list);
        for (final String name : list) {
            debug.appendln(name, actions.get(name));
        }
        /*
         * new ArrayList<E>(actions.keySet().iterator()) Iterator<String> names
         * = Collections.sort().iterator(); while (names.hasNext()) { String
         * name = names.next(); view.appendRow(name, actions.get(name)); }
         */
        final Iterator<Action> iterator = actions.values().iterator();
        while (iterator.hasNext()) {
            iterator.next().debug(debug);
        }

        processors.debug(debug);
    }
}
