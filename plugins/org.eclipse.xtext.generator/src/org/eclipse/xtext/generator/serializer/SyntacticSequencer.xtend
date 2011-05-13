package org.eclipse.xtext.generator.serializer

import org.eclipse.xtext.Grammar
import com.google.inject.Inject

class SyntacticSequencer extends GeneratedFile {
	
	@Inject AbstractSyntacticSequencer sequencer
	
	override String getQualifiedName(Grammar grammar) {
		grammar.getName("", "SyntacticSequencer");		
	}
	
	override String getFileContents() { '''
		package �packageName�;
		
		public class �simpleName� extends �sequencer.simpleName� {
		}
		'''.toString
	}
	
}