/*******************************************************************************
 * Copyright (c) 2000-2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.compiler.lookup;

import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.compiler.problem.AbortCompilation;

/**
 * A parameterized type encapsulates a type with type arguments,
 */
public class ParameterizedTypeBinding extends ReferenceBinding {

	public ReferenceBinding type; 
	public TypeBinding[] arguments;
	public LookupEnvironment environment; // TODO is back pointer actually needed in long term ?
	public char[] genericTypeSignature;
	public ReferenceBinding superclass;
	public ReferenceBinding[] superInterfaces;	
	public FieldBinding[] fields;	
	public MethodBinding[] methods;
	
	public ParameterizedTypeBinding(ReferenceBinding type, TypeBinding[] typeArguments, LookupEnvironment environment){
		
		this.type = type;
		this.fPackage = type.fPackage;
		this.fileName = type.fileName;
		// expect the fields & methods to be initialized correctly later
		this.fields = NoFields;
		this.methods = NoMethods;		
		this.arguments = typeArguments;
		this.environment = environment;
		for (int i = 0, length = typeArguments.length; i < length; i++) {
		    if ((typeArguments[i].tagBits & UseTypeVariable) != 0) {
		        this.tagBits |= UseTypeVariable;
		        break;
		    }
		}
		this.modifiers |= AccUnresolved; // until methods() is sent		
		// TODO determine if need to copy other tagBits from type so as to provide right behavior to all predicates
	}
	
	/**
	 * @see org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding#availableFields()
	 */
	public FieldBinding[] availableFields() {

	    // TODO need to instantiate generic fields 
		return this.type.availableFields();
	}

	/**
	 * @see org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding#availableMethods()
	 */
	public MethodBinding[] availableMethods() {

	    // TODO need to instantiate generic methods
		return this.type.availableMethods();
	}

	/**
	 * @see org.eclipse.jdt.internal.compiler.lookup.TypeBinding#canBeInstantiated()
	 */
	public boolean canBeInstantiated() {

		if (this.type == null) return false;		
		return this.type.canBeInstantiated();
	}

	/**
	 * @see org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding#computeId()
	 */
	public void computeId() {

		this.id = NoId;		
	}

	/**
	 * @see org.eclipse.jdt.internal.compiler.lookup.TypeBinding#constantPoolName()
	 */
	public char[] constantPoolName() {
		return this.type.constantPoolName(); // erasure
	}

	/**
	 * @see org.eclipse.jdt.internal.compiler.lookup.TypeBinding#debugName()
	 */
	String debugName() {
		return this.type.debugName();
	}

	/**
	 * @see org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding#enclosingType()
	 */
	public ReferenceBinding enclosingType() {
		return this.type.enclosingType();
	}

	/**
	 * @see org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding#fieldCount()
	 */
	public int fieldCount() {
		return this.type.fieldCount();
	}

	/**
	 * @see org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding#fields()
	 */
	public FieldBinding[] fields() {
		return this.type.fields();
	}

	/**
	 * Ltype<param1 ... paremN>;
	 * LY<TT;>;
	 */
	public char[] genericTypeSignature() {
	    if (this.genericTypeSignature != null) return this.genericTypeSignature;
	    StringBuffer sig = new StringBuffer(10);
	    sig.append(this.type.genericTypeSignature()).append('<');
	    for (int i = 0, length = this.arguments.length; i < length; i++) {
	        sig.append(this.arguments[i].genericTypeSignature());
	    }
	    sig.append(">;"); //$NON-NLS-1$
		return this.genericTypeSignature = sig.toString().toCharArray();
	}	

	/**
	 * @see org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding#getExactConstructor(TypeBinding[])
	 */
	public MethodBinding getExactConstructor(TypeBinding[] argumentTypes) {
		return this.type.getExactConstructor(argumentTypes);
	}

	/**
	 * @see org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding#getExactMethod(char[], TypeBinding[])
	 */
	public MethodBinding getExactMethod(char[] selector, TypeBinding[] argumentTypes) {
   	    // TODO (kent) need to be optimized to avoid resolving all methods
	    methods();	
		int argCount = argumentTypes.length;
		int selectorLength = selector.length;
		boolean foundNothing = true;
	
		if ((modifiers & AccUnresolved) == 0) { // have resolved all arg types & return type of the methods
			nextMethod : for (int m = methods.length; --m >= 0;) {
				MethodBinding method = methods[m];
				if (method.selector.length == selectorLength && CharOperation.prefixEquals(method.selector, selector)) {
					foundNothing = false; // inner type lookups must know that a method with this name exists
					if (method.parameters.length == argCount) {
						TypeBinding[] toMatch = method.parameters;
						for (int p = 0; p < argCount; p++)
							if (toMatch[p] != argumentTypes[p])
								continue nextMethod;
						return method;
					}
				}
			}
		}
	
		if (foundNothing) {
			if (isInterface()) {
				 if (superInterfaces().length == 1)
					return superInterfaces()[0].getExactMethod(selector, argumentTypes);
			} else if (superclass() != null) {
				return superclass().getExactMethod(selector, argumentTypes);
			}
		}
		return null;
	}

	/**
	 * @see org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding#getField(char[], boolean)
	 */
	public FieldBinding getField(char[] fieldName, boolean needResolve) {
		return this.type.getField(fieldName, needResolve);
	}

	/**
	 * @see org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding#getMemberType(char[])
	 */
	public ReferenceBinding getMemberType(char[] typeName) {
		return this.type.getMemberType(typeName);
	}

	/**
	 * @see org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding#getMethods(char[])
	 */
	public MethodBinding[] getMethods(char[] selector) {
	    // TODO (kent) need to be optimized to avoid resolving all methods
	    methods();	    
		try{
			int count = 0;
			int lastIndex = -1;
			int selectorLength = selector.length;
			if ((modifiers & AccUnresolved) == 0) { // have resolved all arg types & return type of the methods
				for (int m = 0, length = methods.length; m < length; m++) {
					MethodBinding method = methods[m];
					if (method.selector.length == selectorLength && CharOperation.prefixEquals(method.selector, selector)) {
						count++;
						lastIndex = m;
					}
				}
			}
			if (count == 1)
				return new MethodBinding[] {methods[lastIndex]};
			if (count > 1) {
				MethodBinding[] result = new MethodBinding[count];
				count = 0;
				for (int m = 0; m <= lastIndex; m++) {
					MethodBinding method = methods[m];
					if (method.selector.length == selectorLength && CharOperation.prefixEquals(method.selector, selector))
						result[count++] = method;
				}
				return result;
			}
		} catch(AbortCompilation e){
			// ensure null methods are removed
			MethodBinding[] newMethods = null;
			int count = 0;
			for (int i = 0, max = methods.length; i < max; i++){
				MethodBinding method = methods[i];
				if (method == null && newMethods == null){
					System.arraycopy(methods, 0, newMethods = new MethodBinding[max], 0, i);
				} else if (newMethods != null && method != null) {
					newMethods[count++] = method;
				}
			}
			if (newMethods != null){
				System.arraycopy(newMethods, 0, methods = new MethodBinding[count], 0, count);
			}			
			modifiers ^= AccUnresolved;
			throw e;
		}		
		return NoMethods;
	}

	/**
	 * @see org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding#implementsInterface(ReferenceBinding, boolean)
	 */
	public boolean implementsInterface(ReferenceBinding anInterface, boolean searchHierarchy) {
		return this.type.implementsInterface(anInterface, searchHierarchy);
	}

	/**
	 * @see org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding#implementsMethod(MethodBinding)
	 */
	public boolean implementsMethod(MethodBinding method) {
		return this.type.implementsMethod(method);
	}

	/**
	 * @see org.eclipse.jdt.internal.compiler.lookup.TypeBinding#isCompatibleWith(TypeBinding)
	 */
	public boolean isCompatibleWith(TypeBinding right) {
		return this.type.isCompatibleWith(right);
	}

	/**
	 * @see org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding#isSuperclassOf(ReferenceBinding)
	 */
	public boolean isSuperclassOf(ReferenceBinding referenceTypeBinding) {
		return this.type.isSuperclassOf(referenceTypeBinding);
	}
	
	/**
	 * @see org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding#memberTypes()
	 */
	public ReferenceBinding[] memberTypes() {
		return this.type.memberTypes();
	}

	/**
	 * @see org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding#methods()
	 */
	public MethodBinding[] methods() {
	    // TODO (kent) check handling of corner cases (AbortCompilation)
		if ((this.modifiers & AccUnresolved) == 0)
			return this.methods;
	
	    MethodBinding[] originalMethods = this.type.methods();
	    MethodBinding[] substitutedMethods = originalMethods;
	    for (int i = 0, length = originalMethods.length; i < length; i++) {
	        MethodBinding originalMethod = originalMethods[i];
	        if ((originalMethod.modifiers & AccUseTypeVariable) != 0) {
	            if (substitutedMethods == originalMethods) {
	                substitutedMethods = new MethodBinding[length];
	                System.arraycopy(originalMethods, 0, substitutedMethods, 0, i);
	            }
	            // TODO (philippe) should only create new method if substitution did actually occur
	            substitutedMethods[i] = new SubstitutedMethodBinding(originalMethod, this);
	        } else if (originalMethods != substitutedMethods) {
	            substitutedMethods[i] = originalMethod;
	        }
	    }
		modifiers ^= AccUnresolved;
	    return this.methods = substitutedMethods;
	}

	/**
	 * @see org.eclipse.jdt.internal.compiler.lookup.TypeBinding#qualifiedSourceName()
	 */
	public char[] qualifiedSourceName() {
		return this.type.qualifiedSourceName();
	}

	/**
	 * @see org.eclipse.jdt.internal.compiler.lookup.Binding#readableName()
	 */
	public char[] readableName() {
	    StringBuffer nameBuffer = new StringBuffer(10);
		if (this.type.isMemberType()) {
			nameBuffer.append(CharOperation.concat(this.type.enclosingType().readableName(), sourceName, '.'));
		} else {
			nameBuffer.append(CharOperation.concatWith(this.type.compoundName, '.'));
		}	    
		nameBuffer.append('<');
	    for (int i = 0, length = this.arguments.length; i < length; i++) {
	        if (i > 0) nameBuffer.append(',');
	        nameBuffer.append(this.arguments[i].readableName());
	    }
	    nameBuffer.append('>');
	    return nameBuffer.toString().toCharArray();
	}
		
	/**
	 * @see org.eclipse.jdt.internal.compiler.lookup.Binding#shortReadableName()
	 */
	public char[] shortReadableName() {
	    StringBuffer nameBuffer = new StringBuffer(10);
		if (this.type.isMemberType()) {
			nameBuffer.append(CharOperation.concat(this.type.enclosingType().shortReadableName(), sourceName, '.'));
		} else {
			nameBuffer.append(this.type.sourceName);
		}	    
		nameBuffer.append('<');
	    for (int i = 0, length = this.arguments.length; i < length; i++) {
	        if (i > 0) nameBuffer.append(',');
	        nameBuffer.append(this.arguments[i].shortReadableName());
	    }
	    nameBuffer.append('>');
	    return nameBuffer.toString().toCharArray();
	}

	/**
	 * @see org.eclipse.jdt.internal.compiler.lookup.TypeBinding#signature()
	 */
	public char[] signature() {
		return this.type.signature();
	}

	/**
	 * @see org.eclipse.jdt.internal.compiler.lookup.TypeBinding#sourceName()
	 */
	public char[] sourceName() {
		return this.type.sourceName();
	}

	/**
	 * Returns an array of types, where original types got substituted using the receiver
	 * parameterized type.
	 */
	public TypeBinding substitute(TypeBinding originalType) {
	    
	    if ((originalType.tagBits & TagBits.UseTypeVariable) != 0) {
		    if (originalType instanceof TypeVariableBinding) {
		        TypeVariableBinding originalVariable = (TypeVariableBinding) originalType;
		        TypeVariableBinding[] typeVariables = this.type.typeVariables();
		        int length = typeVariables.length;
		        // check this variable can be substituted given parameterized type
		        if (originalVariable.rank < length && typeVariables[originalVariable.rank] == originalVariable) {
		            return this.arguments[originalVariable.rank];
		        }		        
		    } else if (originalType instanceof ParameterizedTypeBinding) {
		        ParameterizedTypeBinding originalParameterizedType = (ParameterizedTypeBinding) originalType;
		        TypeBinding[] originalArguments = originalParameterizedType.arguments;
		        TypeBinding[] substitutedArguments = substitute(originalArguments);
		        if (substitutedArguments != originalArguments) {
		            return this.environment.createParameterizedType(originalParameterizedType.type, substitutedArguments);
		        }
		    }
	    }
	    return originalType;
	}
	
	/**
	 * Returns an array of types, where original types got substituted using the receiver
	 * parameterized type. Only allocate an array if anything is different.
	 */
	public TypeBinding[] substitute(TypeBinding[] originalTypes) {
	    TypeBinding[] substitutedTypes = originalTypes;
	    for (int i = 0, length = originalTypes.length; i < length; i++) {
	        TypeBinding originalType = originalTypes[i];
	        TypeBinding substitutedParameter = this.substitute(originalType);
	        if (substitutedParameter != originalType) {
	            if (substitutedTypes == originalTypes) {
	                substitutedTypes = new TypeBinding[length];
	                System.arraycopy(originalTypes, 0, substitutedTypes, 0, i);
	            }
	            substitutedTypes[i] = substitutedParameter;
	        } else if (substitutedTypes != originalTypes) {
	            substitutedTypes[i] = originalType;
	        }
	    }
	    return substitutedTypes;
    }
	
	/**
	 * @see org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding#superclass()
	 */
	public ReferenceBinding superclass() {
	    if (this.superclass == null) {
	        // note: Object cannot be generic
		    this.superclass = (ReferenceBinding) substitute(this.type.superclass());
	    }
		return this.superclass;
	}

	/**
	 * @see org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding#superInterfaces()
	 */
	public ReferenceBinding[] superInterfaces() {
		return this.type.superInterfaces();
	}

	/**
	 * @see org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding#syntheticEnclosingInstanceTypes()
	 */
	public ReferenceBinding[] syntheticEnclosingInstanceTypes() {
		return this.type.syntheticEnclosingInstanceTypes();
	}

	/**
	 * @see org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding#syntheticOuterLocalVariables()
	 */
	public SyntheticArgumentBinding[] syntheticOuterLocalVariables() {
		return this.type.syntheticOuterLocalVariables();
	}

	/**
	 * @see org.eclipse.jdt.internal.compiler.lookup.TypeBinding#leafComponentType()
	 */
	public TypeBinding leafComponentType() {
		return this.type.leafComponentType();
	}

	/**
	 * @see org.eclipse.jdt.internal.compiler.lookup.TypeBinding#qualifiedPackageName()
	 */
	public char[] qualifiedPackageName() {
		return this.type.qualifiedPackageName();
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		StringBuffer buffer = new StringBuffer(10);
		buffer.append(this.type);
		buffer.append('<');
		for (int i = 0; i < this.arguments.length; i++){
			if (i > 0) buffer.append(',');
			buffer.append(this.arguments[i]);
		}
		buffer.append('>');
		return buffer.toString();
	}
}
