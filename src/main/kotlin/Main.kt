import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.singleWindowApplication
import kotlin.random.Random

sealed class ScreenNode {
    data class Leaf(var color: Color) : ScreenNode()
    data class Internal(var left: ScreenNode, var right: ScreenNode, val isVertical: Boolean) : ScreenNode()
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
    var draggingNode by remember { mutableStateOf<ScreenNode.Leaf?>(null) }
    var targetNode by remember { mutableStateOf<ScreenNode?>(null) }
    var insertPosition by remember { mutableStateOf<InsertPosition?>(null) }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val width = maxWidth
        val height = maxHeight

        Box(Modifier.fillMaxSize()) {
            DisplayNode(
                node = root,
                xOffset = 0.dp,
                yOffset = 0.dp,
                width = width,
                height = height,
                draggingNode = draggingNode,
                onStartDrag = { draggingNode = it as? ScreenNode.Leaf },
                onEndDrag = {
                    if (draggingNode != null && targetNode != null && insertPosition != null) {
                        root = insertNode(root, targetNode!!, draggingNode!!, insertPosition!!)
                    }
                    draggingNode = null
                    targetNode = null
                    insertPosition = null
                },
                onEnterTarget = { targetNode = it },
                onLeaveTarget = { targetNode = null },
                onInsertPosition = { insertPosition = it },
                onSplitNode = { node, isVertical ->
                    root = splitNode(root, node, isVertical)
                },
                onRemoveNode = { node ->
                    root = removeNode(root, node)
                },
                isRoot = true
            )
        }
    }
}

enum class InsertPosition {
    Left, Right, Top, Bottom, Replace
}

@Composable
fun DisplayNode(
    node: ScreenNode,
    xOffset: Dp,
    yOffset: Dp,
    width: Dp,
    height: Dp,
    draggingNode: ScreenNode.Leaf?,
    onStartDrag: (ScreenNode) -> Unit,
    onEndDrag: () -> Unit,
    onEnterTarget: (ScreenNode) -> Unit,
    onLeaveTarget: () -> Unit,
    onInsertPosition: (InsertPosition?) -> Unit,
    onSplitNode: (ScreenNode, Boolean) -> Unit,
    onRemoveNode: (ScreenNode) -> Unit,
    isRoot: Boolean = false,
) {
    when (node) {
        is ScreenNode.Leaf -> {
            DraggableTile(
                tileColor = node.color,
                width = width,
                height = height,
                xOffset = xOffset,
                yOffset = yOffset,
                onStartDrag = { onStartDrag(node) },
                onEndDrag = onEndDrag,
                onEnterTarget = { onEnterTarget(node) },
                onLeaveTarget = onLeaveTarget,
                onInsertPosition = onInsertPosition,
                onSplitTile = { isVertical -> onSplitNode(node, isVertical) },
                onRemoveTile = { onRemoveNode(node) },
                isRoot = isRoot
            )
        }

        is ScreenNode.Internal -> {
            if (node.isVertical) {
                val halfWidth = width / 2
                DisplayNode(
                    node.left,
                    xOffset,
                    yOffset,
                    halfWidth,
                    height,
                    draggingNode,
                    onStartDrag,
                    onEndDrag,
                    onEnterTarget,
                    onLeaveTarget,
                    onInsertPosition,
                    onSplitNode,
                    onRemoveNode
                )
                DisplayNode(
                    node.right,
                    xOffset + halfWidth,
                    yOffset,
                    halfWidth,
                    height,
                    draggingNode,
                    onStartDrag,
                    onEndDrag,
                    onEnterTarget,
                    onLeaveTarget,
                    onInsertPosition,
                    onSplitNode,
                    onRemoveNode
                )
            } else {
                val halfHeight = height / 2
                DisplayNode(
                    node.left,
                    xOffset,
                    yOffset,
                    width,
                    halfHeight,
                    draggingNode,
                    onStartDrag,
                    onEndDrag,
                    onEnterTarget,
                    onLeaveTarget,
                    onInsertPosition,
                    onSplitNode,
                    onRemoveNode
                )
                DisplayNode(
                    node.right,
                    xOffset,
                    yOffset + halfHeight,
                    width,
                    halfHeight,
                    draggingNode,
                    onStartDrag,
                    onEndDrag,
                    onEnterTarget,
                    onLeaveTarget,
                    onInsertPosition,
                    onSplitNode,
                    onRemoveNode
                )
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
    onStartDrag: () -> Unit,
    onEndDrag: () -> Unit,
    onEnterTarget: () -> Unit,
    onLeaveTarget: () -> Unit,
    onInsertPosition: (InsertPosition?) -> Unit,
    onSplitTile: (Boolean) -> Unit,
    onRemoveTile: () -> Unit,
    isRoot: Boolean = false,
) {
    var offset by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = Modifier
            .offset(xOffset + Dp(offset.x), yOffset + Dp(offset.y))
            .size(width, height)
            .background(tileColor, RoundedCornerShape(8.dp))
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { onStartDrag() },
                    onDragEnd = {
                        onEndDrag()
                        offset = Offset.Zero
                    },
                    onDragCancel = { onLeaveTarget() },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        offset += dragAmount
                        val halfWidth = width.toPx() / 2
                        val halfHeight = height.toPx() / 2
                        val centerX = xOffset.toPx() + halfWidth
                        val centerY = yOffset.toPx() + halfHeight
                        val dragX = centerX + offset.x
                        val dragY = centerY + offset.y

                        onInsertPosition(
                            when {
                                dragX < centerX - halfWidth -> InsertPosition.Left
                                dragX > centerX + halfWidth -> InsertPosition.Right
                                dragY < centerY - halfHeight -> InsertPosition.Top
                                dragY > centerY + halfHeight -> InsertPosition.Bottom
                                else -> InsertPosition.Replace
                            }
                        )
                    }
                )
            }
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
                if (!isRoot) {
                    Button(onClick = onRemoveTile) {
                        Icon(Icons.Filled.Close, contentDescription = "Close")
                    }
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
            root.copy(
                left = splitNode(root.left, targetNode, isVertical),
                right = splitNode(root.right, targetNode, isVertical)
            )
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
            when {
                root.left == targetNode -> root.right
                root.right == targetNode -> root.left
                else -> {
                    root.copy(
                        left = removeNode(root.left, targetNode),
                        right = removeNode(root.right, targetNode)
                    )
                }
            }
        }
    }
}

fun insertNode(
    root: ScreenNode,
    targetNode: ScreenNode,
    draggingNode: ScreenNode.Leaf,
    position: InsertPosition,
): ScreenNode {
    return when (root) {
        is ScreenNode.Leaf -> {
            if (root == targetNode) {
                val newLeaf = ScreenNode.Leaf(draggingNode.color)
                when (position) {
                    InsertPosition.Left -> ScreenNode.Internal(newLeaf, root, true)
                    InsertPosition.Right -> ScreenNode.Internal(root, newLeaf, true)
                    InsertPosition.Top -> ScreenNode.Internal(newLeaf, root, false)
                    InsertPosition.Bottom -> ScreenNode.Internal(root, newLeaf, false)
                    InsertPosition.Replace -> draggingNode
                }
            } else {
                root
            }
        }

        is ScreenNode.Internal -> {
            root.copy(
                left = insertNode(root.left, targetNode, draggingNode, position),
                right = insertNode(root.right, targetNode, draggingNode, position)
            )
        }
    }
}

fun main() = singleWindowApplication {
    TileApp()
}
