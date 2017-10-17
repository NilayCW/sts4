/*******************************************************************************
 * Copyright (c) 2017 Pivotal, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal, Inc. - initial API and implementation
 *******************************************************************************/
package org.springframework.ide.vscode.boot.java.beans.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.NodeFinder;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.MarkedString;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.junit.Before;
import org.junit.Test;
import org.springframework.ide.vscode.boot.java.BootJavaLanguageServer;
import org.springframework.ide.vscode.boot.java.autowired.SpringBootAppProvider;
import org.springframework.ide.vscode.boot.java.beans.ComponentHoverProvider;
import org.springframework.ide.vscode.commons.java.IClasspath;
import org.springframework.ide.vscode.commons.java.IJavaProject;
import org.springframework.ide.vscode.commons.languageserver.java.CompositeJavaProjectFinder;
import org.springframework.ide.vscode.commons.util.BadLocationException;
import org.springframework.ide.vscode.commons.util.text.LanguageId;
import org.springframework.ide.vscode.commons.util.text.TextDocument;
import org.springframework.ide.vscode.languageserver.testharness.LanguageServerHarness;
import org.springframework.ide.vscode.project.harness.ProjectsHarness;
import org.springframework.ide.vscode.project.harness.PropertyIndexHarness;

/**
 * @author Martin Lippert
 */
public class ComponentHoverProviderTest {

	private CompositeJavaProjectFinder projectFinder;
	private LanguageServerHarness<BootJavaLanguageServer> harness;
	private PropertyIndexHarness indexHarness;

	@Before
	public void setup() throws Exception {
		projectFinder = new CompositeJavaProjectFinder();

		indexHarness = new PropertyIndexHarness();
		harness = new LanguageServerHarness<BootJavaLanguageServer>(new Callable<BootJavaLanguageServer>() {
			@Override
			public BootJavaLanguageServer call() throws Exception {
				BootJavaLanguageServer server = new BootJavaLanguageServer(projectFinder, indexHarness.getIndexProvider());
				return server;
			}
		}) {
			@Override
			protected String getFileExtension() {
				return ".java";
			}
		};
	}

	@Test
	public void testLiveHoverHintForAutomaicallywiredConstructor() throws Exception {
		File directory = new File(ProjectsHarness.class.getResource("/test-projects/test-annotation-indexing-autowired/").toURI());
		harness.intialize(directory);

		String docURI = "file://" + directory.getAbsolutePath() + "/src/main/java/org/test/MyAutomaticallyWiredComponent.java";
		TextDocument document = createTempTextDocument(docURI);
		IJavaProject project = projectFinder.find(new TextDocumentIdentifier(docURI));

		CompilationUnit cu = parse(document, project);

		int offset = document.toOffset(new Position(4, 2));
		ASTNode node = NodeFinder.perform(cu, offset, 0).getParent();

		ComponentHoverProvider provider = new ComponentHoverProvider();
		String beansJSON = new String(Files.readAllBytes(new File(directory, "runtime-bean-information-automatically-wired.json").toPath()));

		Range hint = provider.getLiveHoverHint((Annotation)node, document, beansJSON);
		assertNotNull(hint);

		assertEquals(10, hint.getStart().getLine());
		assertEquals(8, hint.getStart().getCharacter());
		assertEquals(10, hint.getEnd().getLine());
		assertEquals(37, hint.getEnd().getCharacter());
	}

	@Test
	public void testNoLiveHoverHintForComponentWhenAutowiredAnnotationIsUsed() throws Exception {
		File directory = new File(ProjectsHarness.class.getResource("/test-projects/test-annotation-indexing-autowired/").toURI());
		harness.intialize(directory);

		String docURI = "file://" + directory.getAbsolutePath() + "/src/main/java/org/test/MyAutowiredComponent.java";
		TextDocument document = createTempTextDocument(docURI);
		IJavaProject project = projectFinder.find(new TextDocumentIdentifier(docURI));

		CompilationUnit cu = parse(document, project);

		int offset = document.toOffset(new Position(5, 2));
		ASTNode node = NodeFinder.perform(cu, offset, 0).getParent();

		ComponentHoverProvider provider = new ComponentHoverProvider();
		String beansJSON = new String(Files.readAllBytes(new File(directory, "runtime-bean-information.json").toPath()));

		Range hint = provider.getLiveHoverHint((Annotation)node, document, beansJSON);
		assertNull(hint);
	}

	@Test
	public void testLiveHoverContentForAutomaicallywiredConstructor() throws Exception {
		File directory = new File(ProjectsHarness.class.getResource("/test-projects/test-annotation-indexing-autowired/").toURI());
		harness.intialize(directory);

		String docURI = "file://" + directory.getAbsolutePath() + "/src/main/java/org/test/MyAutomaticallyWiredComponent.java";
		TextDocument document = createTempTextDocument(docURI);
		IJavaProject project = projectFinder.find(new TextDocumentIdentifier(docURI));

		CompilationUnit cu = parse(document, project);

		int offset = document.toOffset(new Position(4, 2));
		ASTNode node = NodeFinder.perform(cu, offset, 0).getParent();

		ComponentHoverProvider provider = new ComponentHoverProvider();
		String beansJSON = new String(Files.readAllBytes(new File(directory, "runtime-bean-information-automatically-wired.json").toPath()));

		SpringBootAppProvider bootApp = new SpringBootAppProvider() {
			@Override
			public String getProcessName() {
				return "test process name";
			}

			@Override
			public String getProcessID() {
				return "test process id";
			}

			@Override
			public String getBeans() throws Exception {
				return beansJSON;
			}
		};
		CompletableFuture<Hover> hoverFuture = provider.provideHover(null, (Annotation) node, null, 0, document, new SpringBootAppProvider[] {bootApp});
		Hover hover = hoverFuture.get();

		assertNotNull(hover);

		assertEquals(10, hover.getRange().getStart().getLine());
		assertEquals(8, hover.getRange().getStart().getCharacter());
		assertEquals(10, hover.getRange().getEnd().getLine());
		assertEquals(37, hover.getRange().getEnd().getCharacter());

		List<Either<String, MarkedString>> contents = hover.getContents();
		assertEquals(6, contents.size());

		assertTrue(contents.get(0).getLeft().contains("myAutomaticallyWiredComponent"));
		assertTrue(contents.get(2).getLeft().contains("dependencyA"));
		assertTrue(contents.get(3).getLeft().contains("dependencyB"));
		assertTrue(contents.get(4).getLeft().contains("test process id"));
		assertTrue(contents.get(5).getLeft().contains("test process name"));
	}

	private TextDocument createTempTextDocument(String docURI) throws Exception {
		Path path = Paths.get(new URI(docURI));
		String content = new String(Files.readAllBytes(path));

		TextDocument doc = new TextDocument(docURI, LanguageId.PLAINTEXT, 0, content);
		return doc;
	}

	private CompilationUnit parse(TextDocument document, IJavaProject project)
			throws Exception, BadLocationException {
		ASTParser parser = ASTParser.newParser(AST.JLS8);
		Map<String, String> options = JavaCore.getOptions();
		JavaCore.setComplianceOptions(JavaCore.VERSION_1_8, options);
		parser.setCompilerOptions(options);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		parser.setStatementsRecovery(true);
		parser.setBindingsRecovery(true);
		parser.setResolveBindings(true);

		String[] classpathEntries = getClasspathEntries(project);
		String[] sourceEntries = new String[] {};
		parser.setEnvironment(classpathEntries, sourceEntries, null, true);

		String docURI = document.getUri();
		String unitName = docURI.substring(docURI.lastIndexOf("/"));
		parser.setUnitName(unitName);
		parser.setSource(document.get(0, document.getLength()).toCharArray());

		CompilationUnit cu = (CompilationUnit) parser.createAST(null);
		return cu;
	}

	private String[] getClasspathEntries(IJavaProject project) throws Exception {
		IClasspath classpath = project.getClasspath();
		Stream<Path> classpathEntries = classpath.getClasspathEntries();
		return classpathEntries
				.filter(path -> path.toFile().exists())
				.map(path -> path.toAbsolutePath().toString()).toArray(String[]::new);
	}


}