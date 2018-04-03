grammar DependenciesTxt;

file : (def)* EOF;

def : moduleDef | dependencyDef;

moduleDef : ID attrs?;
dependencyDef: moduleRef '->' (moduleRef attrs?)?;

attrs: '[' (attr (',' attr)*)? ']';
attr: attrFlagRef | attrKeyValue;
attrKeyValue: attrKeyRef '=' attrValueRef;

moduleRef: ID;
attrFlagRef: ID;
attrKeyRef: ID;
attrValueRef: ID;

ID: LETTER ( LETTER | DIGIT )*;

fragment LETTER: [a-zA-Z\u0080-\u00FF_];
fragment DIGIT: [0-9];

COMMENT: '/*' .*? '*/' -> skip;
LINE_COMMENT: '//' .*? '\r'? '\n' -> skip;

WS: [ \t\n\r]+ -> skip;
NEWLINE  :  ('\r' '\n'|'\r'|'\n'|'\u000C') -> skip;