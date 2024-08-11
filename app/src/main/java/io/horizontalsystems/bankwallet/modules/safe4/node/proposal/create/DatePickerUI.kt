package io.horizontalsystems.bankwallet.modules.safe4.node.proposal.create

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import java.text.SimpleDateFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SingleButtonDatePickerView(
		modifier: Modifier = Modifier,
		value: String = "选择时间",
		onValueChange: (Long) -> Unit,
		borderColor: Color = ComposeAppTheme.colors.jacob,
) {
	var isOpenDialog by remember { mutableStateOf(false) }
	var isDefaultValue by remember { mutableStateOf(true) }
	var selectDate by remember { mutableStateOf(value) }
	val datePickerState = rememberDatePickerState()
	val sdf = remember { SimpleDateFormat("yyy-MM-dd") }
	Row(
			modifier = modifier
					.height(50.dp)
					.border(
							width = 1.dp,
							color = borderColor,
							shape = RoundedCornerShape(4.dp)
					)
					.clickable { isOpenDialog = true },
			verticalAlignment = Alignment.CenterVertically,
			horizontalArrangement = Arrangement.SpaceBetween
	) {
		Text(
				modifier = Modifier.padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 8.dp),
				text = selectDate,
				maxLines = 1,
				color = if(isDefaultValue) ComposeAppTheme.colors.grey else ComposeAppTheme.colors.jacob,
				fontSize = 14.sp,
		)
		Icon(
				modifier = Modifier.padding(start = 5.dp),
				imageVector = Icons.Default.DateRange,
				contentDescription = null,
		)
	}
	if (isOpenDialog) {
		DatePickerDialog(
				onDismissRequest = { isOpenDialog = false},
				colors = DatePickerDefaults.colors(containerColor = ComposeAppTheme.colors.white),   //背景色（这个在下面的DatePicker中设置无效）
				confirmButton = {
					Text(
							modifier = Modifier
									.clickable {
										val date = datePickerState.selectedDateMillis
										if (date != null) {
											onValueChange(date)
											selectDate = sdf.format(date)
											isDefaultValue = false
										} else {
											isDefaultValue = true
										}
										isOpenDialog = false
									}
									.padding(end = 20.dp),
							text = "确定",
							color = ComposeAppTheme.colors.jacob,
							fontSize = 16.sp
					)
				},
				dismissButton = {
					Text(
							modifier = Modifier
									.clickable {
										isOpenDialog = false
									}
									.padding(end = 20.dp),
							text = "取消",
							color = ComposeAppTheme.colors.grey,
							fontSize = 16.sp
					)
				}
		) {
			DatePicker(
					state = datePickerState,
					colors = DatePickerDefaults.colors(
							todayDateBorderColor = ComposeAppTheme.colors.grey,   //默认选中的当天日期的边框色
							selectedDayContentColor = ComposeAppTheme.colors.white,  //选中的文字颜色
							selectedDayContainerColor = ComposeAppTheme.colors.grey,  //选中的填充颜色
					)
			)
		}
	}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TwoButtonDatePickerView(
		modifier: Modifier = Modifier,
		startDate: String = "开始时间",
		endDate: String = "结束时间",
		onStartChange: (Long) -> Unit,
		onEndChange: (Long) -> Unit,
		borderColor: Color = ComposeAppTheme.colors.jacob,
) {
	var isOpenStartDialog by remember { mutableStateOf(false) }
	var isOpenEndDialog by remember { mutableStateOf(false) }
	val startDatePickerState = rememberDatePickerState()
	val endDatePickerState = rememberDatePickerState()
	var isStartDateDefault by remember { mutableStateOf(true) }
	var isEndDateDefault by remember { mutableStateOf(true) }
	var selectStartDate by remember { mutableStateOf(startDate) }
	var selectEndDate by remember { mutableStateOf(endDate) }
	val sdf = remember { SimpleDateFormat("yyy-MM-dd") }
	Row(
			modifier = modifier
					.height(50.dp)
					.border(
							width = 1.dp,
							color = borderColor,
							shape = RoundedCornerShape(4.dp)
					),
			verticalAlignment = Alignment.CenterVertically,
	) {
		Text(
				modifier = Modifier
						.padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 8.dp)
						.clickable { isOpenStartDialog = true },
				text = selectStartDate,
				color = if(isStartDateDefault) ComposeAppTheme.colors.grey else ComposeAppTheme.colors.jacob,
				maxLines = 1,
				fontSize = 14.sp
		)
		Icon(
				modifier = Modifier
						.size(25.dp)
						.padding(horizontal = 5.dp),
				painter = painterResource(id = R.drawable.ic_arrow_right),
				contentDescription = null,
				tint = ComposeAppTheme.colors.grey,
		)
		Text(
				modifier = Modifier
						.padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 8.dp)
						.clickable { isOpenEndDialog = true },
				text = selectEndDate,
				color = if (isEndDateDefault)  ComposeAppTheme.colors.grey else ComposeAppTheme.colors.jacob,
				maxLines = 1,
				fontSize = 14.sp
		)
		Icon(
				modifier = Modifier.padding(start = 5.dp),
				imageVector = Icons.Default.DateRange,
				contentDescription = null,
				tint = ComposeAppTheme.colors.grey
		)
	}
	if (isOpenStartDialog) {
		DatePickerDialog(
				onDismissRequest = { isOpenStartDialog = false},
				colors = DatePickerDefaults.colors(containerColor = ComposeAppTheme.colors.white),
				confirmButton = {
					Text(
							modifier = Modifier
									.clickable {
										val date = startDatePickerState.selectedDateMillis
										if (date != null) {
											onStartChange(date)
											selectStartDate = sdf.format(date)
											isStartDateDefault = false
										} else {
											isStartDateDefault = true
										}
										isOpenStartDialog = false
									}
									.padding(end = 20.dp),
							text = "确定",
							color = ComposeAppTheme.colors.jacob,
							fontSize = 16.sp
					)
				},
				dismissButton = {
					Text(
							modifier = Modifier
									.clickable {
										isOpenStartDialog = false
									}
									.padding(end = 20.dp),
							text = "取消",
							color = ComposeAppTheme.colors.grey,
							fontSize = 16.sp
					)
				}
		) {
			DatePicker(
					state = startDatePickerState,
					colors = DatePickerDefaults.colors(
							todayDateBorderColor = ComposeAppTheme.colors.grey,   //默认选中的当天日期的边框色
							selectedDayContentColor = ComposeAppTheme.colors.white,  //选中的文字颜色
							selectedDayContainerColor = ComposeAppTheme.colors.grey,  //选中的填充颜色
					)
			)
		}
	}
	if (isOpenEndDialog) {
		DatePickerDialog(
				onDismissRequest = { isOpenEndDialog = false},
				colors = DatePickerDefaults.colors(containerColor = ComposeAppTheme.colors.white),
				confirmButton = {
					Text(
							modifier = Modifier
									.clickable {
										val date = endDatePickerState.selectedDateMillis
										if (date != null) {
											onEndChange(date)
											selectEndDate = sdf.format(date)
											isEndDateDefault = false
										} else {
											isEndDateDefault = true
										}
										isOpenEndDialog = false
									}
									.padding(end = 20.dp),
							text = "确定",
							color = ComposeAppTheme.colors.jacob,
							fontSize = 14.sp
					)
				},
				dismissButton = {
					Text(
							modifier = Modifier
									.clickable {
										isOpenEndDialog = false
									}
									.padding(end = 20.dp),
							text = "取消",
							color = ComposeAppTheme.colors.grey,
							fontSize = 16.sp
					)
				}
		) {
			DatePicker(
					state = endDatePickerState,
					colors = DatePickerDefaults.colors(
							containerColor = ComposeAppTheme.colors.white,
							todayDateBorderColor = ComposeAppTheme.colors.grey,
							selectedDayContentColor = ComposeAppTheme.colors.white,
							selectedDayContainerColor = ComposeAppTheme.colors.grey,
					)
			)
		}
	}
}

/*@Composable
fun DatePicker(
		state: DatePickerState,
		modifier: Modifier = Modifier,
		dateFormatter: DatePickerFormatter = remember { DatePickerFormatter() },
		dateValidator: (Long) -> Boolean = { true },
		title: (@Composable () -> Unit)? = {
			DatePickerDefaults.DatePickerTitle(
					state,
					modifier = Modifier.padding(16.dp)
			)
		},
		headline: (@Composable () -> Unit)? = {
			DatePickerDefaults.DatePickerHeadline(
					state,
					dateFormatter,
					modifier = Modifier.padding(16.dp)
			)
		},
		showModeToggle: Boolean = true,
		colors: DatePickerColors = DatePickerDefaults.colors(

				todayDateBorderColor = Color.Red        //默认选中的当天日期的边框色

						selectedDayContentColor = Color.Red        //选中的文字颜色

						selectedDayContainerColor  = Color.Red        //选中的填充颜色

		)
)*/

/*@Composable
fun rememberDatePickerState(
		@Suppress("AutoBoxing") initialSelectedDateMillis: Long? = null,        //默认选中的日期
		@Suppress("AutoBoxing") initialDisplayedMonthMillis: Long? = initialSelectedDateMillis,        //默认显示的月份
		yearRange: IntRange = DatePickerDefaults.YearRange,        //限制选择的年份范围，如 2000..2100
		initialDisplayMode: DisplayMode = DisplayMode.Picker        //选择模式Picker、输入模式Input
): DatePickerState*/
