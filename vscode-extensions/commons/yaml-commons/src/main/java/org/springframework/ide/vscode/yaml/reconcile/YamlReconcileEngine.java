package org.springframework.ide.vscode.yaml.reconcile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ide.vscode.commons.reconcile.IDocument;
import org.springframework.ide.vscode.commons.reconcile.IProblemCollector;
import org.springframework.ide.vscode.commons.reconcile.IReconcileEngine;
import org.springframework.ide.vscode.commons.reconcile.ReconcileProblem;
import org.springframework.ide.vscode.yaml.ast.YamlASTProvider;
import org.springframework.ide.vscode.yaml.ast.YamlFileAST;
import org.yaml.snakeyaml.error.Mark;
import org.yaml.snakeyaml.parser.ParserException;
import org.yaml.snakeyaml.scanner.ScannerException;

/**
 * @author Kris De Volder
 */
public abstract class YamlReconcileEngine implements IReconcileEngine {
	
	final static Logger logger = LoggerFactory.getLogger(YamlReconcileEngine.class);

	protected final YamlASTProvider parser;

	public YamlReconcileEngine(YamlASTProvider parser) {
		this.parser = parser;
	}

	@Override
	public void reconcile(IDocument doc, IProblemCollector problemCollector) {
		problemCollector.beginCollecting();
		try {
			YamlFileAST ast = parser.getAST(doc);
			YamlASTReconciler reconciler = getASTReconciler(doc, problemCollector);
			if (reconciler!=null) {
				reconciler.reconcile(ast);
			}
		} catch (ParserException e) {
			String msg = e.getProblem();
			Mark mark = e.getProblemMark();
			problemCollector.accept(syntaxError(msg, mark.getIndex(), 1));
		} catch (ScannerException e) {
			String msg = e.getProblem();
			Mark mark = e.getProblemMark();
			problemCollector.accept(syntaxError(msg, mark.getIndex(), 1));
		} catch (Exception e) {
			logger.error("unexpected error during reconcile", e);
		} finally {
			problemCollector.endCollecting();
		}
	}

	protected abstract ReconcileProblem syntaxError(String msg, int offset, int length);
	protected abstract YamlASTReconciler getASTReconciler(IDocument doc, IProblemCollector problemCollector);
}
