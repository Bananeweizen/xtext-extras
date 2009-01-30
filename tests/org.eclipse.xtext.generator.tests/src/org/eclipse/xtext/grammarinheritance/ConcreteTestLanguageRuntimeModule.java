/*
Generated with Xtext
*/
package org.eclipse.xtext.grammarinheritance;

import org.eclipse.xtext.conversion.IValueConverterService;

import com.google.inject.Binder;

/**
 * used to register components to be used at runtime.
 */
public class ConcreteTestLanguageRuntimeModule extends AbstractConcreteTestLanguageRuntimeModule {

	@Override
	protected Class<? extends IValueConverterService> getIValueConverterService() {
		return AbstractTestLanguageValueConverters.class;
	}
	
	@Override
	public void configure(Binder binder) {
		super.configure(binder);
	}
}
