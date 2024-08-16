package io.horizontalsystems.bankwallet.modules.safe4.node

import com.google.android.exoplayer2.util.Log
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.HSCaution
import io.horizontalsystems.bankwallet.core.LocalizedException
import io.horizontalsystems.bankwallet.modules.safe4.node.proposal.ProposalInfo
import io.horizontalsystems.bankwallet.modules.safe4.node.proposal.ProposalStatus
import io.horizontalsystems.bankwallet.modules.safe4.node.proposal.ProposalViewItem
import io.horizontalsystems.bankwallet.modules.safe4.node.proposal.info.ProposalInfoViewItem
import io.horizontalsystems.bankwallet.modules.safe4.node.proposal.info.ProposalVote
import io.horizontalsystems.bankwallet.modules.safe4.node.proposal.info.ProposalVoteStatus
import io.horizontalsystems.bankwallet.modules.safe4.node.vote.LockIdsInfo
import io.horizontalsystems.bankwallet.modules.safe4.node.vote.LockIdsView
import io.horizontalsystems.bankwallet.modules.safe4.node.vote.VoteRecordInfo
import io.horizontalsystems.bankwallet.modules.safe4.node.vote.VoteRecordView
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import org.apache.commons.lang3.time.DateFormatUtils
import java.math.BigDecimal
import java.math.BigInteger
import java.net.UnknownHostException
import java.text.DateFormat
import java.text.SimpleDateFormat

object NodeCovertFactory {

	val Super_Node_Create_Amount = 5000
	val Master_Node_Create_Amount = 1000

	val Super_Node_Create_Join_Amount = 1000
	val Master_Node_Create_Join_Amount = 200

	val Node_Lock_Day = 720

	fun createNoteItemView(index: Int, nodeItem: NodeInfo, isSuperNode: Boolean, isRegisterNode: Boolean = false): NodeViewItem {
		val totalVoteNum = valueConvert(nodeItem.totalVoteNum)
		val totalAmount = valueConvert(nodeItem.totalAmount)
		val allVoteNum = valueConvert(nodeItem.allVoteNum)

		val progress = try {
			totalVoteNum.toFloat() / allVoteNum.toFloat()
		} catch (e: Exception) {
			0
		}
		val creatorTotalAmount = valueConvert( nodeItem.founders.sumOf { it.amount })
		val canJoin = if (isSuperNode)
			if (isRegisterNode) false else creatorTotalAmount.toInt() < Super_Node_Create_Amount
		else
			if (isRegisterNode) false else creatorTotalAmount.toInt() < Master_Node_Create_Amount

		val createPledge = if (isSuperNode)
			Super_Node_Create_Amount
		else
			Master_Node_Create_Amount

		return NodeViewItem(
				index + 1,
				nodeItem.id,
				nodeItem.name,
				nodeItem.description,
				App.numberFormatter.formatCoinFull(totalVoteNum, null, 2),
				App.numberFormatter.formatCoinFull(if (isSuperNode) totalAmount else creatorTotalAmount, "SAFE", 2),
				progress.toFloat(),
				"${"%.2f".format((progress).toFloat() * 100)}%",
				nodeItem.addr,
				nodeItem.creator,
				nodeItem.state,
				nodeItem.enode,
				createPledge = "$createPledge SAFE",
				canJoin = canJoin,
				isEdit = nodeItem.isEdit
		)
	}


	fun covertVoteRecord(voteRecords: List<VoteRecordInfo>?): List<VoteRecordView>? {
		return voteRecords?.mapIndexed { index, voteRecordInfo ->
			VoteRecordView(
					index,
					voteRecordInfo.address,
					App.numberFormatter.formatCoinFull(valueConvert(voteRecordInfo.lockValue), "SAFE", 2)
			)
		}
	}

	fun convertLockIdItemView(list: List<LockIdsInfo>?, lockList: List<LockIdsInfo>?): List<LockIdsView>? {
		val mergeList = mutableListOf<LockIdsInfo>()
		lockList?.let {
			mergeList.addAll(it)
		}
		list?.let {
			mergeList.addAll(it)
		}
		return mergeList.distinctBy { it.lockId }.sortedByDescending { it.lockId }
			.mapIndexed { index, record ->
			val value = valueConvert(record.lockValue)
			LockIdsView(
					index,
					record.lockId.toString(),
					App.numberFormatter.formatCoinFull(value, "SAFE", 2),
					record.enable,
					record.checked
			)
		}
	}

	fun convertCreatorList(nodeItem: NodeInfo?): List<CreateViewItem> {
		nodeItem ?: return emptyList()
		return nodeItem.founders.map {
			CreateViewItem(
					it.lockID.toString(),
					it.addr.hex,
					App.numberFormatter.formatCoinFull(valueConvert(it.amount), "SAFE", 2),
			)
		}
	}


	fun createProposalItemView(index: Int, info: ProposalInfo): ProposalViewItem {
		return ProposalViewItem(
				index,
				info.id,
				info.title,
				info.description,
				info.creator,
				ProposalStatus.getStatus(info.state),
				App.numberFormatter.formatCoinFull(valueConvert(info.payAmount), "SAFE", 2),
				DateFormatUtils.format(info.endPayTime * 1000, "yyyy-MM-dd HH:mm:ss")
		)
	}

	fun createProposalInfoItemView(info: ProposalInfo, voteRecord: List<ProposalVote>?): ProposalInfoViewItem {
		val agreeNum = voteRecord?.filter { it.state == 0 }?.size ?: 0
		val rejectNum = voteRecord?.filter { it.state == 1 }?.size ?: 0
		val abstentionNum = voteRecord?.filter { it.state == 2 }?.size ?: 0
		return ProposalInfoViewItem(
				info.id,
				info.title,
				info.description,
				info.creator,
				ProposalStatus.getStatus(info.state),
				info.payTimes.toInt(),
				App.numberFormatter.formatCoinFull(valueConvert(info.payAmount), "SAFE", 2),
				DateFormatUtils.format(info.startPayTime * 1000, "yyyy-MM-dd HH:mm:ss"),
				DateFormatUtils.format(info.endPayTime * 1000, "yyyy-MM-dd HH:mm:ss"),
				agreeNum,
				rejectNum,
				abstentionNum
		)
	}

	fun valueConvert(value: BigInteger): BigDecimal {
		return value.toBigDecimal().movePointLeft(18).stripTrailingZeros()
	}

	fun scaleConvert(value: Int): BigInteger {
		return value.toBigDecimal().movePointRight(18).stripTrailingZeros().toBigInteger()
	}

	fun formatSafe(value: BigInteger): String {
		val decimal = valueConvert(value)
		return App.numberFormatter.formatCoinFull(decimal, "SAFE", 4)
	}

	fun formatDate(time: Long): String {
		val sampleFormat = SimpleDateFormat("yyy-MM-dd hh:mm:ss")
		return sampleFormat.format(time)
	}

	fun createCaution(error: Throwable) = when (error) {
		is UnknownHostException -> HSCaution(TranslatableString.ResString(R.string.Hud_Text_NoInternet))
		is LocalizedException -> HSCaution(TranslatableString.ResString(error.errorTextRes))
		else -> HSCaution(TranslatableString.PlainString(error.message ?: ""))
	}
}