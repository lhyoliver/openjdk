/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package build.tools.module;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import jdk.jigsaw.module.Module;
import jdk.jigsaw.module.ModuleLibrary;
import jdk.jigsaw.module.Resolution;
import jdk.jigsaw.module.SimpleResolver;

public class ModuleUtils {
    private static final String MODULES_SER = "jdk/jigsaw/module/resources/modules.ser";
    public static Module[] readModules()
        throws IOException
    {
        InputStream stream = ClassLoader.getSystemResourceAsStream(MODULES_SER);
        if (stream == null) {
            System.err.format("WARNING: %s not found%n", MODULES_SER);
            return new Module[0];
        }
        try (InputStream in = stream) {
            return readModules(in);
        }
    }

    public static Module[] readModules(InputStream in)
        throws IOException
    {
        try (ObjectInputStream ois = new ObjectInputStream(in)) {
            return (Module[]) ois.readObject();
        } catch (ClassNotFoundException e) {
            throw new Error(e);
        }
    }

    public static Set<Module> resolve(Module[] modules, Set<String> roots)
        throws IOException
    {
        JdkModuleLibrary mlib = new JdkModuleLibrary(modules);
        SimpleResolver resolver = new SimpleResolver(mlib);

        for (String mn : roots) {
            Module m = mlib.findLocalModule(mn);
            if (m == null) {
                throw new Error("module " + mn + " not found");
            }
        }

        Resolution r = resolver.resolve(roots);
        return r.selectedModules();
    }

    private static class JdkModuleLibrary extends ModuleLibrary {
        private final Set<Module> modules = new HashSet<>();
        private final Map<String, Module> namesToModules = new HashMap<>();
        JdkModuleLibrary(Module... mods) {
            for (Module m: mods) {
                modules.add(m);
                namesToModules.put(m.id().name(), m);
            }
        }

        @Override
        public Module findLocalModule(String name) {
            return namesToModules.get(name);
        }

        @Override
        public Set<Module> localModules() {
            return Collections.unmodifiableSet(modules);
        }
    }
}
