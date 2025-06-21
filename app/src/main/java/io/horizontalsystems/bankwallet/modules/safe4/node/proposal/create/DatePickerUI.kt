package io.horizontalsystems.bankwallet.modules.safe4.node.proposal.create

import CustomDatePickerDialog
import android.app.TimePickerDialog
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerColors
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachIndexed
import androidx.compose.ui.window.DialogProperties
import androidx.glance.layout.Spacer
import com.google.android.exoplayer2.util.Log
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.modules.theme.ThemeType
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.HSpacer
import java.text.SimpleDateFormat
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.util.Calendar
import java.util.Date
import java.util.TimeZone
import java.util.stream.IntStream.range
import kotlin.math.max
import kotlin.streams.toList


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ButtonDatePickerView(
	modifier: Modifier = Modifier,
	value: String = "选择时间",
	onValueChange: (Long) -> Unit,
	borderColor: Color = ComposeAppTheme.colors.jacob,
) {
	var isOpenDialog by remember { mutableStateOf(false) }
	var isDefaultValue by remember { mutableStateOf(true) }
	var selectDate by remember { mutableStateOf(value) }
	val datePickerState = rememberDatePickerState(
		selectableDates = object : SelectableDates {
			override fun isSelectableDate(utcTimeMillis: Long): Boolean {
				val calendar = Calendar.getInstance()
				return calendar.timeInMillis + (24 - calendar.get(Calendar.HOUR_OF_DAY))*60*60*1000 < utcTimeMillis
			}

			override fun isSelectableYear(year: Int): Boolean {
				return year >= Calendar.getInstance().get(Calendar.YEAR)
			}
		}
	)
	val sdf = remember { SimpleDateFormat("yyyy-MM-dd") }
	val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
	Row(
		modifier = modifier
			.fillMaxWidth()
			.padding(1.dp)
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
		CustomDatePickerDialog(
			onDismissRequest = { isOpenDialog = false},
			colors = DatePickerDefaults.colors(containerColor = ComposeAppTheme.colors.white),   //背景色（这个在下面的DatePicker中设置无效）
			confirmButton = {
				Text(
					modifier = Modifier
						.clickable {
							val date = datePickerState.selectedDateMillis
							if (date != null) {
								val formatDate = sdf.format(date)
								selectDate = formatDate
								onValueChange(date)
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
			Column {

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
}

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
	val datePickerState = rememberDatePickerState(
			selectableDates = object : SelectableDates {
				override fun isSelectableDate(utcTimeMillis: Long): Boolean {
					val calendar = Calendar.getInstance()
					return calendar.timeInMillis + (24 - calendar.get(Calendar.HOUR_OF_DAY))*60*60*1000 < utcTimeMillis
				}

				override fun isSelectableYear(year: Int): Boolean {
					return year >= Calendar.getInstance().get(Calendar.YEAR)
				}
			}
	)
	var hour by remember { mutableStateOf("00") }
	var minute by remember { mutableStateOf("00") }
	var second by remember { mutableStateOf("00") }

	val sdf = remember { SimpleDateFormat("yyyy-MM-dd") }
	val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
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
		CustomDatePickerDialog(
				onDismissRequest = { isOpenDialog = false},
				colors = DatePickerDefaults.colors(containerColor = ComposeAppTheme.colors.white),   //背景色（这个在下面的DatePicker中设置无效）
				confirmButton = {
					Text(
							modifier = Modifier
									.clickable {
										val date = datePickerState.selectedDateMillis
										if (date != null) {
											val formatDate = sdf.format(date)
											selectDate = "$formatDate $hour:$minute:$second"
											onValueChange(dateFormat.parse(selectDate).time)
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
			Column {

				DatePicker(
						state = datePickerState,
						colors = DatePickerDefaults.colors(
								todayDateBorderColor = ComposeAppTheme.colors.grey,   //默认选中的当天日期的边框色
								selectedDayContentColor = ComposeAppTheme.colors.white,  //选中的文字颜色
								selectedDayContainerColor = ComposeAppTheme.colors.grey,  //选中的填充颜色
						)
				)
				Column(
						modifier = Modifier.height(200.dp)
				) {
					val time = "$hour:$minute:$second"
					Text(
							modifier = Modifier.padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 8.dp),
							text = time,
							maxLines = 1,
							color = ComposeAppTheme.colors.bran ,
							fontSize = 16.sp,
					)
					Row(modifier = Modifier.padding(start = 16.dp, end = 16.dp),) {
						TimeList(timeList = range(0, 24).toList(),
								selectIndex = hour.toInt(),
								onValueSelect = {
									hour = it
								}
						)
						HSpacer(16.dp)
						Divider(
								modifier = Modifier.width(1.dp).fillMaxHeight(),
								thickness = 1.dp,
								color = ComposeAppTheme.colors.steel10,
						)
						HSpacer(16.dp)
						TimeList(timeList = range(0, 60).toList(),
								selectIndex = minute.toInt(),
								onValueSelect = {
									minute = it
								}
						)
						HSpacer(16.dp)
						Divider(
								modifier = Modifier.width(1.dp).fillMaxHeight(),
								thickness = 1.dp,
								color = ComposeAppTheme.colors.steel10,
						)
						HSpacer(16.dp)
						TimeList(timeList = range(0, 60).toList(),
								selectIndex = second.toInt(),
								onValueSelect = {
									second = it
								}
						)
					}
				}
			}
		}
	}
}


@Composable
fun TimeList(
		timeList: List<Int>,
		selectIndex: Int,
		onValueSelect: (String) -> Unit
) {
	val state = rememberScrollState()
	LaunchedEffect(Unit) { state.animateScrollTo(selectIndex * 80) }

	Column(
			modifier = Modifier.verticalScroll(state)
	) {
		timeList.forEachIndexed { index, time ->
			val text = "${if (time < 10) 0 else ""}$time"
			Text(
					modifier = Modifier
							.padding()
							.clip(RoundedCornerShape(8.dp))
							.background(
									if (index == selectIndex)
										ComposeAppTheme.colors.green50
									else
										ComposeAppTheme.colors.transparent)
							.padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 8.dp)
							.clickable {
								onValueSelect.invoke(text)
							},
					text = text,
					maxLines = 1,
					color = ComposeAppTheme.colors.bran,
					fontSize = 14.sp,
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
	val startDatePickerState = rememberDatePickerState(
			selectableDates = object : SelectableDates {
				override fun isSelectableDate(utcTimeMillis: Long): Boolean {
					val calendar = Calendar.getInstance()
					return calendar.timeInMillis + (24 - calendar.get(Calendar.HOUR_OF_DAY))*60*60*1000 < utcTimeMillis
				}

				override fun isSelectableYear(year: Int): Boolean {
					return year >= Calendar.getInstance().get(Calendar.YEAR)
				}
			}
	)
	val endDatePickerState = rememberDatePickerState(
			selectableDates = object : SelectableDates {
				override fun isSelectableDate(utcTimeMillis: Long): Boolean {
					val calendar = Calendar.getInstance()
					return calendar.timeInMillis + (24 - calendar.get(Calendar.HOUR_OF_DAY))*60*60*1000 < utcTimeMillis
				}

				override fun isSelectableYear(year: Int): Boolean {
					return year >= Calendar.getInstance().get(Calendar.YEAR)
				}
			}
	)
	var isStartDateDefault by remember { mutableStateOf(true) }
	var isEndDateDefault by remember { mutableStateOf(true) }
	var selectStartDate by remember { mutableStateOf(startDate) }
	var selectEndDate by remember { mutableStateOf(endDate) }

	var startHour by remember { mutableStateOf("00") }
	var startMinute by remember { mutableStateOf("00") }
	var startSecond by remember { mutableStateOf("00") }

	var endHour by remember { mutableStateOf("00") }
	var endMinute by remember { mutableStateOf("00") }
	var endSecond by remember { mutableStateOf("00") }

	val sdf = remember { SimpleDateFormat("yyyy-MM-dd") }
	val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

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
						.padding(start = 8.dp, top = 8.dp, end = 8.dp, bottom = 8.dp)
						.clickable { isOpenStartDialog = true },
				text = selectStartDate,
				color = if(isStartDateDefault) ComposeAppTheme.colors.grey else ComposeAppTheme.colors.jacob,
				maxLines = 1,
				fontSize = 14.sp
		)
		Icon(
				modifier = Modifier
						.size(20.dp)
						.padding(horizontal = 5.dp),
				painter = painterResource(id = R.drawable.ic_arrow_right),
				contentDescription = null,
				tint = ComposeAppTheme.colors.grey,
		)
		Text(
				modifier = Modifier
						.padding(start = 8.dp, top = 8.dp, end = 8.dp, bottom = 8.dp)
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
		CustomDatePickerDialog(
				onDismissRequest = { isOpenStartDialog = false},
				colors = DatePickerDefaults.colors(containerColor = ComposeAppTheme.colors.white),
				confirmButton = {
					Text(
							modifier = Modifier
									.clickable {
										val date = startDatePickerState.selectedDateMillis
										if (date != null) {
											val formatDate = sdf.format(date)
											selectStartDate = "$formatDate $startHour:$startMinute:$startSecond"
											onStartChange(dateFormat.parse(selectStartDate).time)
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
			Column {
				DatePicker(
						state = startDatePickerState,
						colors = DatePickerDefaults.colors(
								todayDateBorderColor = ComposeAppTheme.colors.grey,   //默认选中的当天日期的边框色
								selectedDayContentColor = ComposeAppTheme.colors.white,  //选中的文字颜色
								selectedDayContainerColor = ComposeAppTheme.colors.grey,  //选中的填充颜色
						)
				)
				Column(
						modifier = Modifier.height(200.dp)
				) {
					val time = "$startHour:$startMinute:$startSecond"
					Text(
							modifier = Modifier.padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 8.dp),
							text = time,
							maxLines = 1,
							color = ComposeAppTheme.colors.bran,
							fontSize = 16.sp,
					)
					Row(modifier = Modifier.padding(start = 16.dp, end = 16.dp), ) {
						TimeList(timeList = range(0, 24).toList(),
								selectIndex = startHour.toInt(),
								onValueSelect = {
									startHour = it
								}
						)
						HSpacer(16.dp)
						Divider(
								modifier = Modifier.width(1.dp).fillMaxHeight(),
								thickness = 1.dp,
								color = ComposeAppTheme.colors.steel10,
						)
						HSpacer(16.dp)
						TimeList(timeList = range(0, 60).toList(),
								selectIndex = startMinute.toInt(),
								onValueSelect = {
									startMinute = it
								}
						)
						HSpacer(16.dp)
						Divider(
								modifier = Modifier.width(1.dp).fillMaxHeight(),
								thickness = 1.dp,
								color = ComposeAppTheme.colors.steel10,
						)
						HSpacer(16.dp)
						TimeList(timeList = range(0, 60).toList(),
								selectIndex = startSecond.toInt(),
								onValueSelect = {
									startSecond = it
								}
						)
					}
				}
			}
		}
	}
	if (isOpenEndDialog) {
		CustomDatePickerDialog(
				onDismissRequest = { isOpenEndDialog = false},
				colors = DatePickerDefaults.colors(containerColor = ComposeAppTheme.colors.white),
				confirmButton = {
					Text(
							modifier = Modifier
									.clickable {
										val date = endDatePickerState.selectedDateMillis
										if (date != null) {
											val formatDate = sdf.format(date)
											selectEndDate = "$formatDate $endHour:$endMinute:$endSecond"
											onEndChange(dateFormat.parse(selectEndDate).time)

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
			Column {
				DatePicker(
						state = endDatePickerState,
						colors = DatePickerDefaults.colors(
								containerColor = ComposeAppTheme.colors.white,
								todayDateBorderColor = ComposeAppTheme.colors.grey,
								selectedDayContentColor = ComposeAppTheme.colors.white,
								selectedDayContainerColor = ComposeAppTheme.colors.grey,
						)
				)
				Column(
						modifier = Modifier.height(200.dp)
				) {
					val time = "$endHour:$endMinute:$endSecond"
					Text(
							modifier = Modifier.padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 8.dp),
							text = time,
							maxLines = 1,
							color = ComposeAppTheme.colors.bran,
							fontSize = 16.sp,
					)
					Row(modifier = Modifier.padding(start = 16.dp, end = 16.dp), ) {
						TimeList(timeList = range(0, 24).toList(),
								selectIndex = endHour.toInt(),
								onValueSelect = {
									endHour = it
								}
						)
						HSpacer(16.dp)
						Divider(
								modifier = Modifier.width(1.dp).fillMaxHeight(),
								thickness = 1.dp,
								color = ComposeAppTheme.colors.steel10,
						)
						HSpacer(16.dp)
						TimeList(timeList = range(0, 60).toList(),
								selectIndex = endMinute.toInt(),
								onValueSelect = {
									endMinute = it
								}
						)
						HSpacer(16.dp)
						Divider(
								modifier = Modifier.width(1.dp).fillMaxHeight(),
								thickness = 1.dp,
								color = ComposeAppTheme.colors.steel10,
						)
						HSpacer(16.dp)
						TimeList(timeList = range(0, 60).toList(),
								selectIndex = endSecond.toInt(),
								onValueSelect = {
									endSecond = it
								}
						)
					}
				}
			}
		}
	}
}
