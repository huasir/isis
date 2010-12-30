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


package org.apache.isis.viewer.scimpi.servlet;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletContext;

import org.apache.log4j.Logger;
import org.apache.isis.core.commons.debug.DebugString;
import org.apache.isis.core.commons.exceptions.IsisException;
import org.apache.isis.core.metamodel.adapter.ObjectAdapter;
import org.apache.isis.core.metamodel.spec.ObjectSpecification;


/**
 * ImageLookup provides an efficient way of finding the most suitable image to use. It ensures that an image
 * is always available, providing a default image if needed. All requests are cached to improve performance.
 */
// TODO allow for multiple extension types
public class ImageLookup {
    private static final Logger LOG = Logger.getLogger(ImageLookup.class);
    private static final String UNKNOWN_IMAGE = "Default";
    private static final String[] EXTENSIONS = { "png", "gif", "jpg", "jpeg" };
    private static final Map images = new HashMap();
    private static String imageDirectory;
    //private static String unknownImageFile;
    private static ServletContext context;

    public static void setImageDirectory(ServletContext context, String imageDirectory) {
        LOG.debug("image directory required for: " + imageDirectory);
        ImageLookup.context = context;
        imageDirectory = (imageDirectory.startsWith("/") ? "" : "/") + imageDirectory + "/";
        Set resourcePaths = context.getResourcePaths(imageDirectory);
        if(resourcePaths == null || resourcePaths.size() == 0) {
            throw new IsisException("No image directory found: " + imageDirectory);
        }
        LOG.info("image directory set to: " + imageDirectory);
        ImageLookup.imageDirectory = imageDirectory;
    }
    
    public static void debug(DebugString debug) {
        debug.appendTitle("Image Lookup");
        debug.indent();
        Iterator keys = images.keySet().iterator();
        while (keys.hasNext()) {
            Object key = keys.next();
            Object value = images.get(key);
            debug.appendln(key + " -> " + value);
        }
        debug.unindent();
    }

    private static String imageFile(final String imageName, String contextPath) {
        for (int i = 0; i < EXTENSIONS.length; i++) {
            URL resource;
            try {
                String imagePath = imageDirectory + imageName + "." + EXTENSIONS[i];
                resource = context.getResource(imagePath);
                if (resource != null) {
                    LOG.debug("image found at " + contextPath + imagePath);
                    return contextPath + imagePath;
                }
                URL onClasspath = ImageLookup.class.getResource(imagePath);
                if (onClasspath != null) {
                    LOG.debug("image found on classpath " + onClasspath);
                    return contextPath + imagePath;
                }
            } catch (MalformedURLException ignore) {
            }
        }
        return null;
    }

    private static String findImage(ObjectSpecification specification, String contextPath) {
        String path = findImageFor(specification, contextPath);
        if (path == null) {
            path = imageFile(UNKNOWN_IMAGE, contextPath);
        }
        return path;
    }

    private static String findImageFor(ObjectSpecification specification, String contextPath) {
        String name = specification.getShortIdentifier();
        String fileName = imageFile(name, contextPath);
        if (fileName != null) {
            images.put(name, fileName);
            return fileName;
        } else {
            for (ObjectSpecification interfaceSpec : specification.interfaces()) {
                String path = findImageFor(interfaceSpec, contextPath);
                if (path != null) {
                    return path;
                }
            }
            ObjectSpecification superclass = specification.superclass();
            if (superclass != null) {
                return findImageFor(superclass, contextPath);
            } else {
                return null;
            }
        }
    }

    /**
     * For an object, the icon name from the object is return if it is not null, otherwise the specification
     * is used to look up a suitable image name.
     * @param contextPath 
     * 
     * @see ObjectAdapter#getIconName()
     * @see #imagePath(ObjectSpecification)
     */
 /*   public static String imagePath(ObjectAdapter object) {
        String iconName = object.getIconName();
        if (iconName != null) {
            return imagePath(iconName);
        } else {
            return imagePath(object.getSpecification());
        }
    }
*/
    public static String imagePath(ObjectSpecification specification, String contextPath) {
        String name = specification.getShortIdentifier();
        String imageName = (String) images.get(name);
        if (imageName != null) {
            return (String) imageName;
        } else {
            return findImage(specification, contextPath);
        }
    }
/*
    public static String imagePath(String name) {
        String imageName = (String) images.get(name);
        if (imageName != null) {
            return (String) imageName;
        } else {
            String fileName = imageFile(name);
            return fileName == null ? unknownImageFile : fileName;
        }
    }
*/

    public static String imagePath(ObjectAdapter object, String contextPath) {
        String name = object.getIconName();
        String imageName = (String) images.get(name);
        if (imageName != null) {
            return (String) imageName;
        } else {
            String imageFile = imageFile(name, contextPath);
            if (imageFile != null) {
                return imageFile;
            } else {
                return findImage(object.getSpecification(), contextPath);
            }
        }
    }
}

