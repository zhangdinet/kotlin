// Generated from /Users/jetbrains/kotlin/jps-plugin/jps-tests/test/org/jetbrains/kotlin/jps/build/dependeciestxt/DependenciesTxt.g4 by ANTLR 4.7
package org.jetbrains.kotlin.jps.build.dependeciestxt.generated;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.*;
import org.antlr.v4.runtime.tree.*;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class DependenciesTxtParser extends Parser {
	static { RuntimeMetaData.checkVersion("4.7", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		T__0=1, T__1=2, T__2=3, T__3=4, T__4=5, ID=6, COMMENT=7, LINE_COMMENT=8, 
		WS=9, NEWLINE=10;
	public static final int
		RULE_file = 0, RULE_def = 1, RULE_moduleDef = 2, RULE_dependencyDef = 3, 
		RULE_attrs = 4, RULE_attr = 5, RULE_attrKeyValue = 6, RULE_moduleRef = 7, 
		RULE_attrFlagRef = 8, RULE_attrKeyRef = 9, RULE_attrValueRef = 10;
	public static final String[] ruleNames = {
		"file", "def", "moduleDef", "dependencyDef", "attrs", "attr", "attrKeyValue", 
		"moduleRef", "attrFlagRef", "attrKeyRef", "attrValueRef"
	};

	private static final String[] _LITERAL_NAMES = {
		null, "'->'", "'['", "','", "']'", "'='"
	};
	private static final String[] _SYMBOLIC_NAMES = {
		null, null, null, null, null, null, "ID", "COMMENT", "LINE_COMMENT", "WS", 
		"NEWLINE"
	};
	public static final Vocabulary VOCABULARY = new VocabularyImpl(_LITERAL_NAMES, _SYMBOLIC_NAMES);

	/**
	 * @deprecated Use {@link #VOCABULARY} instead.
	 */
	@Deprecated
	public static final String[] tokenNames;
	static {
		tokenNames = new String[_SYMBOLIC_NAMES.length];
		for (int i = 0; i < tokenNames.length; i++) {
			tokenNames[i] = VOCABULARY.getLiteralName(i);
			if (tokenNames[i] == null) {
				tokenNames[i] = VOCABULARY.getSymbolicName(i);
			}

			if (tokenNames[i] == null) {
				tokenNames[i] = "<INVALID>";
			}
		}
	}

	@Override
	@Deprecated
	public String[] getTokenNames() {
		return tokenNames;
	}

	@Override

	public Vocabulary getVocabulary() {
		return VOCABULARY;
	}

	@Override
	public String getGrammarFileName() { return "DependenciesTxt.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public ATN getATN() { return _ATN; }

	public DependenciesTxtParser(TokenStream input) {
		super(input);
		_interp = new ParserATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}
	public static class FileContext extends ParserRuleContext {
		public TerminalNode EOF() { return getToken(DependenciesTxtParser.EOF, 0); }
		public List<DefContext> def() {
			return getRuleContexts(DefContext.class);
		}
		public DefContext def(int i) {
			return getRuleContext(DefContext.class,i);
		}
		public FileContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_file; }
	}

	public final FileContext file() throws RecognitionException {
		FileContext _localctx = new FileContext(_ctx, getState());
		enterRule(_localctx, 0, RULE_file);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(25);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==ID) {
				{
				{
				setState(22);
				def();
				}
				}
				setState(27);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(28);
			match(EOF);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class DefContext extends ParserRuleContext {
		public ModuleDefContext moduleDef() {
			return getRuleContext(ModuleDefContext.class,0);
		}
		public DependencyDefContext dependencyDef() {
			return getRuleContext(DependencyDefContext.class,0);
		}
		public DefContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_def; }
	}

	public final DefContext def() throws RecognitionException {
		DefContext _localctx = new DefContext(_ctx, getState());
		enterRule(_localctx, 2, RULE_def);
		try {
			setState(32);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,1,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(30);
				moduleDef();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(31);
				dependencyDef();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ModuleDefContext extends ParserRuleContext {
		public TerminalNode ID() { return getToken(DependenciesTxtParser.ID, 0); }
		public AttrsContext attrs() {
			return getRuleContext(AttrsContext.class,0);
		}
		public ModuleDefContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_moduleDef; }
	}

	public final ModuleDefContext moduleDef() throws RecognitionException {
		ModuleDefContext _localctx = new ModuleDefContext(_ctx, getState());
		enterRule(_localctx, 4, RULE_moduleDef);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(34);
			match(ID);
			setState(36);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==T__1) {
				{
				setState(35);
				attrs();
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class DependencyDefContext extends ParserRuleContext {
		public List<ModuleRefContext> moduleRef() {
			return getRuleContexts(ModuleRefContext.class);
		}
		public ModuleRefContext moduleRef(int i) {
			return getRuleContext(ModuleRefContext.class,i);
		}
		public AttrsContext attrs() {
			return getRuleContext(AttrsContext.class,0);
		}
		public DependencyDefContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_dependencyDef; }
	}

	public final DependencyDefContext dependencyDef() throws RecognitionException {
		DependencyDefContext _localctx = new DependencyDefContext(_ctx, getState());
		enterRule(_localctx, 6, RULE_dependencyDef);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(38);
			moduleRef();
			setState(39);
			match(T__0);
			setState(44);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,4,_ctx) ) {
			case 1:
				{
				setState(40);
				moduleRef();
				setState(42);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==T__1) {
					{
					setState(41);
					attrs();
					}
				}

				}
				break;
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class AttrsContext extends ParserRuleContext {
		public List<AttrContext> attr() {
			return getRuleContexts(AttrContext.class);
		}
		public AttrContext attr(int i) {
			return getRuleContext(AttrContext.class,i);
		}
		public AttrsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_attrs; }
	}

	public final AttrsContext attrs() throws RecognitionException {
		AttrsContext _localctx = new AttrsContext(_ctx, getState());
		enterRule(_localctx, 8, RULE_attrs);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(46);
			match(T__1);
			setState(55);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==ID) {
				{
				setState(47);
				attr();
				setState(52);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==T__2) {
					{
					{
					setState(48);
					match(T__2);
					setState(49);
					attr();
					}
					}
					setState(54);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				}
			}

			setState(57);
			match(T__3);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class AttrContext extends ParserRuleContext {
		public AttrFlagRefContext attrFlagRef() {
			return getRuleContext(AttrFlagRefContext.class,0);
		}
		public AttrKeyValueContext attrKeyValue() {
			return getRuleContext(AttrKeyValueContext.class,0);
		}
		public AttrContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_attr; }
	}

	public final AttrContext attr() throws RecognitionException {
		AttrContext _localctx = new AttrContext(_ctx, getState());
		enterRule(_localctx, 10, RULE_attr);
		try {
			setState(61);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,7,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(59);
				attrFlagRef();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(60);
				attrKeyValue();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class AttrKeyValueContext extends ParserRuleContext {
		public AttrKeyRefContext attrKeyRef() {
			return getRuleContext(AttrKeyRefContext.class,0);
		}
		public AttrValueRefContext attrValueRef() {
			return getRuleContext(AttrValueRefContext.class,0);
		}
		public AttrKeyValueContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_attrKeyValue; }
	}

	public final AttrKeyValueContext attrKeyValue() throws RecognitionException {
		AttrKeyValueContext _localctx = new AttrKeyValueContext(_ctx, getState());
		enterRule(_localctx, 12, RULE_attrKeyValue);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(63);
			attrKeyRef();
			setState(64);
			match(T__4);
			setState(65);
			attrValueRef();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ModuleRefContext extends ParserRuleContext {
		public TerminalNode ID() { return getToken(DependenciesTxtParser.ID, 0); }
		public ModuleRefContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_moduleRef; }
	}

	public final ModuleRefContext moduleRef() throws RecognitionException {
		ModuleRefContext _localctx = new ModuleRefContext(_ctx, getState());
		enterRule(_localctx, 14, RULE_moduleRef);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(67);
			match(ID);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class AttrFlagRefContext extends ParserRuleContext {
		public TerminalNode ID() { return getToken(DependenciesTxtParser.ID, 0); }
		public AttrFlagRefContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_attrFlagRef; }
	}

	public final AttrFlagRefContext attrFlagRef() throws RecognitionException {
		AttrFlagRefContext _localctx = new AttrFlagRefContext(_ctx, getState());
		enterRule(_localctx, 16, RULE_attrFlagRef);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(69);
			match(ID);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class AttrKeyRefContext extends ParserRuleContext {
		public TerminalNode ID() { return getToken(DependenciesTxtParser.ID, 0); }
		public AttrKeyRefContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_attrKeyRef; }
	}

	public final AttrKeyRefContext attrKeyRef() throws RecognitionException {
		AttrKeyRefContext _localctx = new AttrKeyRefContext(_ctx, getState());
		enterRule(_localctx, 18, RULE_attrKeyRef);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(71);
			match(ID);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class AttrValueRefContext extends ParserRuleContext {
		public TerminalNode ID() { return getToken(DependenciesTxtParser.ID, 0); }
		public AttrValueRefContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_attrValueRef; }
	}

	public final AttrValueRefContext attrValueRef() throws RecognitionException {
		AttrValueRefContext _localctx = new AttrValueRefContext(_ctx, getState());
		enterRule(_localctx, 20, RULE_attrValueRef);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(73);
			match(ID);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static final String _serializedATN =
		"\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\3\fN\4\2\t\2\4\3\t"+
		"\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4\13\t\13\4"+
		"\f\t\f\3\2\7\2\32\n\2\f\2\16\2\35\13\2\3\2\3\2\3\3\3\3\5\3#\n\3\3\4\3"+
		"\4\5\4\'\n\4\3\5\3\5\3\5\3\5\5\5-\n\5\5\5/\n\5\3\6\3\6\3\6\3\6\7\6\65"+
		"\n\6\f\6\16\68\13\6\5\6:\n\6\3\6\3\6\3\7\3\7\5\7@\n\7\3\b\3\b\3\b\3\b"+
		"\3\t\3\t\3\n\3\n\3\13\3\13\3\f\3\f\3\f\2\2\r\2\4\6\b\n\f\16\20\22\24\26"+
		"\2\2\2J\2\33\3\2\2\2\4\"\3\2\2\2\6$\3\2\2\2\b(\3\2\2\2\n\60\3\2\2\2\f"+
		"?\3\2\2\2\16A\3\2\2\2\20E\3\2\2\2\22G\3\2\2\2\24I\3\2\2\2\26K\3\2\2\2"+
		"\30\32\5\4\3\2\31\30\3\2\2\2\32\35\3\2\2\2\33\31\3\2\2\2\33\34\3\2\2\2"+
		"\34\36\3\2\2\2\35\33\3\2\2\2\36\37\7\2\2\3\37\3\3\2\2\2 #\5\6\4\2!#\5"+
		"\b\5\2\" \3\2\2\2\"!\3\2\2\2#\5\3\2\2\2$&\7\b\2\2%\'\5\n\6\2&%\3\2\2\2"+
		"&\'\3\2\2\2\'\7\3\2\2\2()\5\20\t\2).\7\3\2\2*,\5\20\t\2+-\5\n\6\2,+\3"+
		"\2\2\2,-\3\2\2\2-/\3\2\2\2.*\3\2\2\2./\3\2\2\2/\t\3\2\2\2\609\7\4\2\2"+
		"\61\66\5\f\7\2\62\63\7\5\2\2\63\65\5\f\7\2\64\62\3\2\2\2\658\3\2\2\2\66"+
		"\64\3\2\2\2\66\67\3\2\2\2\67:\3\2\2\28\66\3\2\2\29\61\3\2\2\29:\3\2\2"+
		"\2:;\3\2\2\2;<\7\6\2\2<\13\3\2\2\2=@\5\22\n\2>@\5\16\b\2?=\3\2\2\2?>\3"+
		"\2\2\2@\r\3\2\2\2AB\5\24\13\2BC\7\7\2\2CD\5\26\f\2D\17\3\2\2\2EF\7\b\2"+
		"\2F\21\3\2\2\2GH\7\b\2\2H\23\3\2\2\2IJ\7\b\2\2J\25\3\2\2\2KL\7\b\2\2L"+
		"\27\3\2\2\2\n\33\"&,.\669?";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}