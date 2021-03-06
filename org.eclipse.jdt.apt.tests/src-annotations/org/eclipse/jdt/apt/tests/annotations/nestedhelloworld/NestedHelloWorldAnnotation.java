/*******************************************************************************
 * Copyright (c) 2005 BEA Systems, Inc. 
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    mkaufman@bea.com - initial API and implementation
 *    
 *******************************************************************************/

package org.eclipse.jdt.apt.tests.annotations.nestedhelloworld;

public @interface NestedHelloWorldAnnotation
{
	String value() default ""; //$NON-NLS-1$
}
