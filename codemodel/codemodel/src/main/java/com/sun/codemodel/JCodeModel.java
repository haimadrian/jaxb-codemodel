/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

package com.sun.codemodel;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.sun.codemodel.writer.FileCodeWriter;
import com.sun.codemodel.writer.ProgressCodeWriter;


/**
 * Root of the code DOM.
 *
 * <p>
 * Here's your typical CodeModel application.
 *
 * <pre>
 * JCodeModel cm = new JCodeModel();
 *
 * // generate source code by populating the 'cm' tree.
 * cm._class(...);
 * ...
 *
 * // write them out
 * cm.build(new File("."));
 * </pre>
 *
 * <p>
 * Every CodeModel node is always owned by one {@link JCodeModel} object
 * at any given time (which can be often accesesd by the <tt>owner()</tt> method.)
 *
 * As such, when you generate Java code, most of the operation works
 * in a top-down fashion. For example, you create a class from {@link JCodeModel},
 * which gives you a {@link JDefinedClass}. Then you invoke a method on it
 * to generate a new method, which gives you {@link JMethod}, and so on.
 *
 * There are a few exceptions to this, most notably building {@link JExpression}s,
 * but generally you work with CodeModel in a top-down fashion.
 *
 * Because of this design, most of the CodeModel classes aren't directly instanciable.
 *
 *
 * <h2>Where to go from here?</h2>
 * <p>
 * Most of the time you'd want to populate new type definitions in a {@link JCodeModel}.
 * See {@link #_class(String, ClassType)}.
 */
public final class JCodeModel {
    
    /** The packages that this JCodeWriter contains. */
    private HashMap<String,JPackage> packages = new HashMap<String,JPackage>();
    
    /** All JReferencedClasses are pooled here. */
    private final HashMap<Class<?>,JReferencedClass> refClasses = new HashMap<Class<?>,JReferencedClass>();

    
    /** Obtains a reference to the special "null" type. */
    public final JNullType NULL = new JNullType(this);
    // primitive types 
    public final JPrimitiveType VOID    = new JPrimitiveType(this,"void",   Void.class);
    public final JPrimitiveType BOOLEAN = new JPrimitiveType(this,"boolean",Boolean.class);
    public final JPrimitiveType BYTE    = new JPrimitiveType(this,"byte",   Byte.class);
    public final JPrimitiveType SHORT   = new JPrimitiveType(this,"short",  Short.class);
    public final JPrimitiveType CHAR    = new JPrimitiveType(this,"char",   Character.class);
    public final JPrimitiveType INT     = new JPrimitiveType(this,"int",    Integer.class);
    public final JPrimitiveType FLOAT   = new JPrimitiveType(this,"float",  Float.class);
    public final JPrimitiveType LONG    = new JPrimitiveType(this,"long",   Long.class);
    public final JPrimitiveType DOUBLE  = new JPrimitiveType(this,"double", Double.class);
    
    /**
     * If the flag is true, we will consider two classes "Foo" and "foo"
     * as a collision.
     */
    protected static final boolean isCaseSensitiveFileSystem = getFileSystemCaseSensitivity();
    
    private static boolean getFileSystemCaseSensitivity() {
        try {
            // let the system property override, in case the user really
            // wants to override.
            if( System.getProperty("com.sun.codemodel.FileSystemCaseSensitive")!=null )
                return true;
        } catch( Exception e ) {}
        
        // on Unix, it's case sensitive.
        return (File.separatorChar == '/');
    }


    public JCodeModel() {}
    
    /**
     * Add a package to the list of packages to be generated
     *
     * @param name
     *        Name of the package. Use "" to indicate the root package.
     *
     * @return Newly generated package
     */
    public JPackage _package(String name) {
        JPackage p = packages.get(name);
        if (p == null) {
            p = new JPackage(name, this);
            packages.put(name, p);
        }
        return p;
    }
    
    public final JPackage rootPackage() {
        return _package("");
    }

    /**
     * Returns an iterator that walks the packages defined using this code
     * writer.
     */
    public Iterator<JPackage> packages() {
        return packages.values().iterator();
    }
    
    /**
     * Creates a new generated class.
     * 
     * @exception JClassAlreadyExistsException
     *      When the specified class/interface was already created.
     */
    public JDefinedClass _class(String fullyqualifiedName) throws JClassAlreadyExistsException {
        return _class(fullyqualifiedName,ClassType.CLASS);
    }

    /**
     * Creates a dummy, unknown {@link JClass} that represents a given name.
     *
     * <p>
     * This method is useful when the code generation needs to include the user-specified
     * class that may or may not exist, and only thing known about it is a class name.
     */
    public JClass directClass(String name) {
        return new JDirectClass(this,name);
    }

    /**
     * Creates a new generated class.
     *
     * @exception JClassAlreadyExistsException
     *      When the specified class/interface was already created.
     */
    public JDefinedClass _class(int mods, String fullyqualifiedName,ClassType t) throws JClassAlreadyExistsException {
        int idx = fullyqualifiedName.lastIndexOf('.');
        if( idx<0 )     return rootPackage()._class(fullyqualifiedName);
        else
            return _package(fullyqualifiedName.substring(0,idx))
                ._class(mods, fullyqualifiedName.substring(idx+1), t );
    }

    /**
     * Creates a new generated class.
     *
     * @exception JClassAlreadyExistsException
     *      When the specified class/interface was already created.
     */
    public JDefinedClass _class(String fullyqualifiedName,ClassType t) throws JClassAlreadyExistsException {
        return _class( JMod.PUBLIC, fullyqualifiedName, t );
    }

    /**
     * Gets a reference to the already created generated class.
     * 
     * @return null
     *      If the class is not yet created.
     * @see JPackage#_getClass(String)
     */
    public JDefinedClass _getClass(String fullyQualifiedName) {
        int idx = fullyQualifiedName.lastIndexOf('.');
        if( idx<0 )     return rootPackage()._getClass(fullyQualifiedName);
        else
            return _package(fullyQualifiedName.substring(0,idx))
                ._getClass( fullyQualifiedName.substring(idx+1) );
    }

    /**
     * Creates a new anonymous class.
     * 
     * @deprecated
     *      The naming convention doesn't match the rest of the CodeModel.
     *      Use {@link #anonymousClass(JClass)} instead.
     */
    public JDefinedClass newAnonymousClass(JClass baseType) {
        return new JAnonymousClass(baseType);
    }

    /**
     * Creates a new anonymous class.
     */
    public JDefinedClass anonymousClass(JClass baseType) {
        return new JAnonymousClass(baseType);
    }

    public JDefinedClass anonymousClass(Class<?> baseType) {
        return anonymousClass(ref(baseType));
    }
    
    /**
     * Generates Java source code.
     * A convenience method for <code>build(destDir,destDir,System.out)</code>.
     * 
     * @param	destDir
     *		source files are generated into this directory.
     * @param   status
     *      if non-null, progress indication will be sent to this stream.
     */
    public void build( File destDir, PrintStream status ) throws IOException {
        build(destDir,destDir,status);
    }

    /**
     * Generates Java source code.
     * A convenience method that calls {@link #build(CodeWriter,CodeWriter)}.
     *
     * @param	srcDir
     *		Java source files are generated into this directory.
     * @param	resourceDir
     *		Other resource files are generated into this directory.
     * @param   status
     *      if non-null, progress indication will be sent to this stream.
     */
    public void build( File srcDir, File resourceDir, PrintStream status ) throws IOException {
        if(status!=null) {
            try (CodeWriter src = new ProgressCodeWriter(new FileCodeWriter(srcDir), status );
                 CodeWriter res = new ProgressCodeWriter(new FileCodeWriter(resourceDir), status )) {
                build(src, res);
            }
        } else {
            try (CodeWriter src = new FileCodeWriter(srcDir);
                 CodeWriter res = new FileCodeWriter(resourceDir)) {
                build(src, res);
            }
        }
    }

    /**
     * A convenience method for <code>build(destDir,System.out)</code>.
     */
    public void build( File destDir ) throws IOException {
        build(destDir,System.out);
    }

    /**
     * A convenience method for <code>build(srcDir,resourceDir,System.out)</code>.
     */
    public void build( File srcDir, File resourceDir ) throws IOException {
        build(srcDir,resourceDir,System.out);
    }

    /**
     * A convenience method for <code>build(out,out)</code>.
     */
    public void build( CodeWriter out ) throws IOException {
        build(out,out);
    }
    
    /**
     * Generates Java source code.
     */
    public void build( CodeWriter source, CodeWriter resource ) throws IOException {
        JPackage[] pkgs = packages.values().toArray(new JPackage[packages.size()]);
        // avoid concurrent modification exception
        for (JPackage pkg : pkgs)
            pkg.build(source, resource);
    }

    /**
     * Returns the number of files to be generated if
     * {@link #build} is invoked now.
     */
    public int countArtifacts() {
        int r = 0;
        JPackage[] pkgs = packages.values().toArray(new JPackage[packages.size()]);
        // avoid concurrent modification exception
        for( JPackage pkg : pkgs )
            r += pkg.countArtifacts();
        return r;
    }

	
    /**
     * Obtains a reference to an existing class from its Class object.
     *
     * <p>
     * The parameter may not be primitive.
     *
     * @see #_ref(Class) for the version that handles more cases.
     */
    public JClass ref(Class<?> clazz) {
        JReferencedClass jrc = (JReferencedClass)refClasses.get(clazz);
        if (jrc == null) {
            if (clazz.isPrimitive())
                throw new IllegalArgumentException(clazz+" is a primitive");
            if (clazz.isArray()) {
                return new JArrayClass(this, _ref(clazz.getComponentType()));
            } else {
                jrc = new JReferencedClass(clazz);
                refClasses.put(clazz, jrc);
            }
        }
        return jrc;
    }

    public JType _ref(Class<?> c) {
        if(c.isPrimitive())
            return JType.parse(this,c.getName());
        else
            return ref(c);
    }

    /**
     * Obtains a reference to an existing class from its fully-qualified
     * class name.
     *
     * <p>
     * First, this method attempts to load the class of the given name.
     * If that fails, we assume that the class is derived straight from
     * {@link Object}, and return a {@link JClass}.
     */
    public JClass ref(String fullyQualifiedClassName) {
        try {
            // try the context class loader first
            return ref(SecureLoader.getContextClassLoader().loadClass(fullyQualifiedClassName));
        } catch (ClassNotFoundException e) {
            // fall through
        }
        // then the default mechanism.
        try {
            return ref(Class.forName(fullyQualifiedClassName));
        } catch (ClassNotFoundException e1) {
            // fall through
        }

        // assume it's not visible to us.
        return new JDirectClass(this,fullyQualifiedClassName);
    }

    /**
     * Cached for {@link #wildcard()}.
     */
    private JClass wildcard;

    /**
     * Gets a {@link JClass} representation for "?",
     * which is equivalent to "? extends Object".
     */
    public JClass wildcard() {
        if(wildcard==null)
            wildcard = ref(Object.class).wildcard();
        return wildcard;
    }

    /**
     * Obtains a type object from a type name.
     *
     * <p>
     * This method handles primitive types, arrays, and existing {@link Class}es.
     *
     * @exception ClassNotFoundException
     *      If the specified type is not found.
     */
    public JType parseType(String name) throws ClassNotFoundException {
        // array
        if(name.endsWith("[]"))
            return parseType(name.substring(0,name.length()-2)).array();

        // try primitive type
        try {
            return JType.parse(this,name);
        } catch (IllegalArgumentException e) {
            ;
        }

        // existing class
        return new TypeNameParser(name).parseTypeName();
    }

    private final class TypeNameParser {
        private final String s;
        private int idx;

        public TypeNameParser(String s) {
            this.s = s;
        }

        /**
         * Parses a type name token T (which can be potentially of the form Tr&ly;T1,T2,...>,
         * or "? extends/super T".)
         *
         * @return the index of the character next to T.
         */
        JClass parseTypeName() throws ClassNotFoundException {
            int start = idx;

            if(s.charAt(idx)=='?') {
                // wildcard
                idx++;
                ws();
                String head = s.substring(idx);
                if(head.startsWith("extends")) {
                    idx+=7;
                    ws();
                    return parseTypeName().wildcard();
                } else
                if(head.startsWith("super")) {
                    throw new UnsupportedOperationException("? super T not implemented");
                } else {
                    // not supported
                    throw new IllegalArgumentException("only extends/super can follow ?, but found "+s.substring(idx));
                }
            }

            while(idx<s.length()) {
                char ch = s.charAt(idx);
                if(Character.isJavaIdentifierStart(ch)
                || Character.isJavaIdentifierPart(ch)
                || ch=='.')
                    idx++;
                else
                    break;
            }

            JClass clazz = ref(s.substring(start,idx));

            return parseSuffix(clazz);
        }

        /**
         * Parses additional left-associative suffixes, like type arguments
         * and array specifiers.
         */
        private JClass parseSuffix(JClass clazz) throws ClassNotFoundException {
            if(idx==s.length())
                return clazz; // hit EOL

            char ch = s.charAt(idx);

            if(ch=='<')
                return parseSuffix(parseArguments(clazz));

            if(ch=='[') {
                if(s.charAt(idx+1)==']') {
                    idx+=2;
                    return parseSuffix(clazz.array());
                }
                throw new IllegalArgumentException("Expected ']' but found "+s.substring(idx+1));
            }

            return clazz;
        }

        /**
         * Skips whitespaces
         */
        private void ws() {
            while(Character.isWhitespace(s.charAt(idx)) && idx<s.length())
                idx++;
        }

        /**
         * Parses '&lt;T1,T2,...,Tn>'
         *
         * @return the index of the character next to '>'
         */
        private JClass parseArguments(JClass rawType) throws ClassNotFoundException {
            if(s.charAt(idx)!='<')
                throw new IllegalArgumentException();
            idx++;

            List<JClass> args = new ArrayList<JClass>();

            while(true) {
                args.add(parseTypeName());
                if(idx==s.length())
                    throw new IllegalArgumentException("Missing '>' in "+s);
                char ch = s.charAt(idx);
                if(ch=='>')
                    return rawType.narrow(args.toArray(new JClass[args.size()]));

                if(ch!=',')
                    throw new IllegalArgumentException(s);
                idx++;
            }

        }
    }

    /**
     * References to existing classes.
     * 
     * <p>
     * JReferencedClass is kept in a pool so that they are shared.
     * There is one pool for each JCodeModel object.
     * 
     * <p>
     * It is impossible to cache JReferencedClass globally only because
     * there is the _package() method, which obtains the owner JPackage
     * object, which is scoped to JCodeModel.
     */
    private class JReferencedClass extends JClass implements JDeclaration {
        private final Class<?> _class;

        JReferencedClass(Class<?> _clazz) {
            super(JCodeModel.this);
            this._class = _clazz;
            assert !_class.isArray();
        }

        public String name() {
            return _class.getSimpleName().replace('$','.');
        }

        public String fullName() {
            return _class.getName().replace('$','.');
        }

        public String binaryName() {
            return _class.getName();
        }

        public JClass outer() {
            Class<?> p = _class.getDeclaringClass();
            if(p==null)     return null;
            return ref(p);
        }

        public JPackage _package() {
            String name = fullName();

            // this type is array
            if (name.indexOf('[') != -1)
                return JCodeModel.this._package("");

            // other normal case
            int idx = name.lastIndexOf('.');
            if (idx < 0)
                return JCodeModel.this._package("");
            else
                return JCodeModel.this._package(name.substring(0, idx));
        }

        public JClass _extends() {
            Class<?> sp = _class.getSuperclass();
            if (sp == null) {
                if(isInterface())
                    return owner().ref(Object.class);
                return null;
            } else
                return ref(sp);
        }

        public Iterator<JClass> _implements() {
            final Class<?>[] interfaces = _class.getInterfaces();
            return new Iterator<JClass>() {
                private int idx = 0;
                public boolean hasNext() {
                    return idx < interfaces.length;
                }
                public JClass next() {
                    return JCodeModel.this.ref(interfaces[idx++]);
                }
                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
        }

        public boolean isInterface() {
            return _class.isInterface();
        }

        public boolean isAbstract() {
            return Modifier.isAbstract(_class.getModifiers());
        }

        public JPrimitiveType getPrimitiveType() {
            Class<?> v = boxToPrimitive.get(_class);
            if(v!=null)
                return JType.parse(JCodeModel.this,v.getName());
            else
                return null;
        }

        public boolean isArray() {
            return false;
        }

        public void declare(JFormatter f) {
        }

        public JTypeVar[] typeParams() {
            // TODO: does JDK 1.5 reflection provides these information?
            return super.typeParams();
        }

        protected JClass substituteParams(JTypeVar[] variables, List<JClass> bindings) {
            // TODO: does JDK 1.5 reflection provides these information?
            return this;
        }
    }

    /**
     * Conversion from primitive type {@link Class} (such as {@link Integer#TYPE}
     * to its boxed type (such as <tt>Integer.class</tt>)
     */
    public static final Map<Class<?>,Class<?>> primitiveToBox;
    /**
     * The reverse look up for {@link #primitiveToBox}
     */
    public static final Map<Class<?>,Class<?>> boxToPrimitive;

    static {
        Map<Class<?>,Class<?>> m1 = new HashMap<Class<?>,Class<?>>();
        Map<Class<?>,Class<?>> m2 = new HashMap<Class<?>,Class<?>>();

        m1.put(Boolean.class,Boolean.TYPE);
        m1.put(Byte.class,Byte.TYPE);
        m1.put(Character.class,Character.TYPE);
        m1.put(Double.class,Double.TYPE);
        m1.put(Float.class,Float.TYPE);
        m1.put(Integer.class,Integer.TYPE);
        m1.put(Long.class,Long.TYPE);
        m1.put(Short.class,Short.TYPE);
        m1.put(Void.class,Void.TYPE);

        for (Map.Entry<Class<?>, Class<?>> e : m1.entrySet())
            m2.put(e.getValue(),e.getKey());

        boxToPrimitive = Collections.unmodifiableMap(m1);
        primitiveToBox = Collections.unmodifiableMap(m2);

    }
}
