import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.singleWindowApplication
import kotlin.random.Random

// Узел дерева для разбиения экрана
sealed class ScreenNode {
    data class Leaf(var color: Color) : ScreenNode()
    data class Internal(val left: ScreenNode, val right: ScreenNode, val isVertical: Boolean) : ScreenNode()
}

@Composable
fun TileApp() {
    var root by remember {
        mutableStateOf<ScreenNode>(
            ScreenNode.Leaf(
                Color(
                    Random.nextInt(256),
                    Random.nextInt(256),
                    Random.nextInt(256)
                )
            )
        )
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val width = maxWidth
        val height = maxHeight

        Box(Modifier.fillMaxSize()) {
            DisplayNode(root, 0.dp, 0.dp, width, height, onSplitNode = { node, isVertical ->
                root = splitNode(root, node, isVertical)
            }, onRemoveNode = { node ->
                root = removeNode(root, node)
            })
        }
    }
}

@Composable
fun DisplayNode(
    node: ScreenNode,
    xOffset: Dp,
    yOffset: Dp,
    width: Dp,
    height: Dp,
    onSplitNode: (ScreenNode, Boolean) -> Unit,
    onRemoveNode: (ScreenNode) -> Unit,
) {
    when (node) {
        is ScreenNode.Leaf -> {
            DraggableTile(
                tileColor = node.color,
                width = width,
                height = height,
                xOffset = xOffset,
                yOffset = yOffset,
                onSplitTile = { isVertical -> onSplitNode(node, isVertical) },
                onRemoveTile = { onRemoveNode(node) }
            )
        }

        is ScreenNode.Internal -> {
            if (node.isVertical) {
                val halfWidth = width / 2
                DisplayNode(node.left, xOffset, yOffset, halfWidth, height, onSplitNode, onRemoveNode)
                DisplayNode(node.right, xOffset + halfWidth, yOffset, halfWidth, height, onSplitNode, onRemoveNode)
            } else {
                val halfHeight = height / 2
                DisplayNode(node.left, xOffset, yOffset, width, halfHeight, onSplitNode, onRemoveNode)
                DisplayNode(node.right, xOffset, yOffset + halfHeight, width, halfHeight, onSplitNode, onRemoveNode)
            }
        }
    }
}

@Composable
fun DraggableTile(
    tileColor: Color,
    width: Dp,
    height: Dp,
    xOffset: Dp,
    yOffset: Dp,
    onSplitTile: (Boolean) -> Unit,
    onRemoveTile: () -> Unit,
) {
    Box(
        modifier = Modifier
            .offset(xOffset, yOffset)
            .size(width, height)
            .background(tileColor, RoundedCornerShape(8.dp))
            .padding(4.dp)
    ) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Button(onClick = { onSplitTile(true) }) {
                    Text("Split V")
                }
                Button(onClick = { onSplitTile(false) }) {
                    Text("Split H")
                }
                Button(onClick = onRemoveTile) {
                    Icon(Icons.Filled.Close, contentDescription = "Close")
                }
            }
        }
    }
}

fun splitNode(root: ScreenNode, targetNode: ScreenNode, isVertical: Boolean): ScreenNode {
    return when (root) {
        is ScreenNode.Leaf -> {
            if (root == targetNode) {
                val newColor1 = Color(Random.nextInt(256), Random.nextInt(256), Random.nextInt(256))
                val newColor2 = Color(Random.nextInt(256), Random.nextInt(256), Random.nextInt(256))
                ScreenNode.Internal(ScreenNode.Leaf(newColor1), ScreenNode.Leaf(newColor2), isVertical)
            } else {
                root
            }
        }

        is ScreenNode.Internal -> {
            val left = splitNode(root.left, targetNode, isVertical)
            val right = splitNode(root.right, targetNode, isVertical)
            ScreenNode.Internal(left, right, root.isVertical)
        }
    }
}

fun removeNode(root: ScreenNode, targetNode: ScreenNode): ScreenNode {
    return when (root) {
        is ScreenNode.Leaf -> {
            if (root == targetNode) {
                ScreenNode.Leaf(Color.White) // Удаление последнего тайла оставляет белое пространство
            } else {
                root
            }
        }

        is ScreenNode.Internal -> {
            val left = removeNode(root.left, targetNode)
            val right = removeNode(root.right, targetNode)
            when {
                left is ScreenNode.Leaf && right is ScreenNode.Leaf -> {
                    // Если оба дочерних узла листья, оставляем один из них
                    left
                }

                else -> {
                    ScreenNode.Internal(left, right, root.isVertical)
                }
            }
        }
    }
}

fun main() = singleWindowApplication {
    TileApp()
}
