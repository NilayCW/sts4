package org.springframework.ide.vscode.yaml.ast;

import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeId;
import org.yaml.snakeyaml.nodes.ScalarNode;

/**
 * @author Kris De Volder
 */
public class NodeUtil {

	/**
	 * Determines whether a node contains the given offset.
	 * The range a node covers between its begin and end mark is treated
	 * as a half open interval. The start offset is treated as included
	 * in the range but the end offset is excluded.
	 * <p>
	 * This is to avoid ambiguity as node ranges tend to 'join'
	 * together so that the end region of one node coincides with
	 * the start region of the next node. By treating node ranges
	 * as 'half open' intervals every offset is typically only
	 * part of two different nodes if those nodes effectively
	 * have overlapping ranges (i.e. only if one node contains
	 * the other). Thus, an operation like finding the smallest
	 * node that contains an offset is unambgious.
	 */
	public static boolean contains(Node node, int offset) {
		return getStart(node)<=offset && offset<getEnd(node);
	}

	public static int getStart(Node node) {
		return node.getStartMark().getIndex();
	}

	public static int getEnd(Node node) {
		return node.getEndMark().getIndex();
	}

	/**
	 * Retrieve String value of a scalar node.
	 * @return String value or null if node is not a Scalar node.
	 */
	public static String asScalar(Node node) {
		if (node.getNodeId()==NodeId.scalar) {
			return ((ScalarNode)node).getValue();
		}
		return null;
	}

}
