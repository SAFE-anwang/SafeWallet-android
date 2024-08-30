import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.DatePickerColors
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachIndexed
import androidx.compose.ui.window.DialogProperties
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import kotlin.math.max

@ExperimentalMaterial3Api
@Composable
fun CustomDatePickerDialog(
		onDismissRequest: () -> Unit,
		confirmButton: @Composable () -> Unit,
		modifier: Modifier = Modifier,
		dismissButton: @Composable (() -> Unit)? = null,
		shape: Shape = DatePickerDefaults.shape,
		tonalElevation: Dp = DatePickerDefaults.TonalElevation,
		colors: DatePickerColors = DatePickerDefaults.colors(),
		properties: DialogProperties = DialogProperties(usePlatformDefaultWidth = false),
		content: @Composable ColumnScope.() -> Unit
) {
	BasicAlertDialog(
			onDismissRequest = onDismissRequest,
			modifier = modifier.wrapContentHeight(),
			properties = properties
	) {
		Surface(
				modifier =
				Modifier.requiredWidth(360.0.dp)
						.heightIn(max = 768.0.dp),
				shape = shape,
				color = colors.containerColor,
				tonalElevation = tonalElevation,
		) {
			Column(verticalArrangement = Arrangement.SpaceBetween) {
				// Wrap the content with a Box and Modifier.weight(1f) to ensure that any "confirm"
				// and "dismiss" buttons are not pushed out of view when running on small screens,
				// or when nesting a DateRangePicker.
				// Fill is false to support collapsing the dialog's height when switching to input
				// mode.
				Box(Modifier.weight(1f, fill = false)) { this@Column.content() }
				// Buttons
				Box(modifier = Modifier.align(Alignment.End).padding(PaddingValues(bottom = 8.dp, end = 6.dp))) {
					CustomProvideContentColorTextStyle(
							contentColor = ComposeAppTheme.colors.lawrence,
							textStyle = TextStyle.Default
					) {
						CustomAlertDialogFlowRow(
								mainAxisSpacing = DialogButtonsMainAxisSpacing,
								crossAxisSpacing = DialogButtonsCrossAxisSpacing
						) {
							dismissButton?.invoke()
							confirmButton()
						}
					}
				}
			}
		}
	}
}

@Composable
fun CustomProvideContentColorTextStyle(
		contentColor: Color,
		textStyle: TextStyle,
		content: @Composable () -> Unit
) {
	val mergedStyle = LocalTextStyle.current.merge(textStyle)
	CompositionLocalProvider(
			LocalContentColor provides contentColor,
			LocalTextStyle provides mergedStyle,
			content = content
	)
}

@Composable
fun CustomAlertDialogFlowRow(
		mainAxisSpacing: Dp,
		crossAxisSpacing: Dp,
		content: @Composable () -> Unit
) {
	Layout(content) { measurables, constraints ->
		val sequences = mutableListOf<List<Placeable>>()
		val crossAxisSizes = mutableListOf<Int>()
		val crossAxisPositions = mutableListOf<Int>()

		var mainAxisSpace = 0
		var crossAxisSpace = 0

		val currentSequence = mutableListOf<Placeable>()
		var currentMainAxisSize = 0
		var currentCrossAxisSize = 0

		// Return whether the placeable can be added to the current sequence.
		fun canAddToCurrentSequence(placeable: Placeable) =
				currentSequence.isEmpty() ||
						currentMainAxisSize + mainAxisSpacing.roundToPx() + placeable.width <=
						constraints.maxWidth

		// Store current sequence information and start a new sequence.
		fun startNewSequence() {
			if (sequences.isNotEmpty()) {
				crossAxisSpace += crossAxisSpacing.roundToPx()
			}
			// Ensures that confirming actions appear above dismissive actions.
			@Suppress("ListIterator") sequences.add(0, currentSequence.toList())
			crossAxisSizes += currentCrossAxisSize
			crossAxisPositions += crossAxisSpace

			crossAxisSpace += currentCrossAxisSize
			mainAxisSpace = max(mainAxisSpace, currentMainAxisSize)

			currentSequence.clear()
			currentMainAxisSize = 0
			currentCrossAxisSize = 0
		}

		measurables.fastForEach { measurable ->
			// Ask the child for its preferred size.
			val placeable = measurable.measure(constraints)

			// Start a new sequence if there is not enough space.
			if (!canAddToCurrentSequence(placeable)) startNewSequence()

			// Add the child to the current sequence.
			if (currentSequence.isNotEmpty()) {
				currentMainAxisSize += mainAxisSpacing.roundToPx()
			}
			currentSequence.add(placeable)
			currentMainAxisSize += placeable.width
			currentCrossAxisSize = max(currentCrossAxisSize, placeable.height)
		}

		if (currentSequence.isNotEmpty()) startNewSequence()

		val mainAxisLayoutSize = max(mainAxisSpace, constraints.minWidth)

		val crossAxisLayoutSize = max(crossAxisSpace, constraints.minHeight)

		val layoutWidth = mainAxisLayoutSize

		val layoutHeight = crossAxisLayoutSize

		layout(layoutWidth, layoutHeight) {
			sequences.fastForEachIndexed { i, placeables ->
				val childrenMainAxisSizes =
						IntArray(placeables.size) { j ->
							placeables[j].width +
									if (j < placeables.lastIndex) mainAxisSpacing.roundToPx() else 0
						}
				val arrangement = Arrangement.End
				val mainAxisPositions = IntArray(childrenMainAxisSizes.size) { 0 }
				with(arrangement) {
					arrange(
							mainAxisLayoutSize,
							childrenMainAxisSizes,
							layoutDirection,
							mainAxisPositions
					)
				}
				placeables.fastForEachIndexed { j, placeable ->
					placeable.place(x = mainAxisPositions[j], y = crossAxisPositions[i])
				}
			}
		}
	}
}


private val DialogButtonsPadding = PaddingValues(bottom = 8.dp, end = 6.dp)
private val DialogButtonsMainAxisSpacing = 8.dp
private val DialogButtonsCrossAxisSpacing = 12.dp