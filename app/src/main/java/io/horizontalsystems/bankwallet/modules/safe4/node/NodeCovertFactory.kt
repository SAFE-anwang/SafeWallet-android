package io.horizontalsystems.bankwallet.modules.safe4.node

import com.anwang.types.masternode.MasterNodeInfo
import com.anwang.types.supernode.SuperNodeInfo
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
import io.horizontalsystems.ethereumkit.models.Address
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

	// 加入合伙人的最低数量
	val Super_Node_Partner_Join_Amount = 500
	val Master_Node_Partner_Join_Amount = 100

	val Node_Lock_Day = 720

	// 每天产块数量
	val DAILY_BLOCK_PRODUCTION_SPEED = 2880

	fun createNoteItemView(index: Int, nodeItem: NodeInfo,
						   isSuperNode: Boolean,
						   isSuperOrMasterNode: Boolean = false,
						   isCreatorNode: Boolean = false, receiveAddress: String? = null): NodeViewItem {
		val totalVoteNum = valueConvert(nodeItem.totalVoteNum)
		val totalAmount = valueConvert(nodeItem.totalAmount)
		val allVoteNum = valueConvert(nodeItem.allVoteNum)

		val progress = try {
			if (allVoteNum.toInt() > 0) {
				totalVoteNum.toFloat() / allVoteNum.toFloat()
			} else {
				0
			}
		} catch (e: Exception) {
			0
		}
		val creatorTotalAmount = valueConvert( nodeItem.founders.sumOf { it.amount })
		// 加入规则： 加入超级节点S4时，调用者不是S4，也不可以在是超级节点和主节点。
		val isPledgeAll = if (isSuperNode)
			creatorTotalAmount.toInt() < Super_Node_Create_Amount
		else
			creatorTotalAmount.toInt() < Master_Node_Create_Amount

		val canJoin = if (isSuperNode)
			!isSuperOrMasterNode && nodeItem.addr.hex != receiveAddress && isPledgeAll
		else
			!isSuperOrMasterNode && nodeItem.addr.hex != receiveAddress && isPledgeAll

		val createPledge = if (isSuperNode)
			Super_Node_Create_Amount
		else
			Master_Node_Create_Amount
		val isPartner = nodeItem.founders.any { it.addr.hex == receiveAddress }
		return NodeViewItem(
				index + 1,
				nodeItem.id,
				nodeItem.name,
				nodeItem.description,
				App.numberFormatter.formatCoinFull(totalVoteNum, null, 2),
				App.numberFormatter.formatCoinFull(if (isSuperNode) totalAmount else creatorTotalAmount, null, 2),
				progress.toFloat(),
				"${"%.2f".format((progress).toFloat() * 100)}%",
				nodeItem.addr,
				nodeItem.creator,
				nodeItem.state,
				nodeItem.founders,
				nodeItem.enode,
				createPledge = "${creatorTotalAmount.toInt()} SAFE",
				canJoin = canJoin,
				isEdit = nodeItem.isEdit,
				isMine = receiveAddress == nodeItem.addr.hex || receiveAddress == nodeItem.creator.hex || isPartner,
				isVoteEnable = !isPledgeAll && !isCreatorNode && receiveAddress != nodeItem.addr.hex,
				isPartner = isPartner && receiveAddress != nodeItem.creator.hex,
				isCreator = receiveAddress == nodeItem.creator.hex,
				isAddLockDay = receiveAddress == nodeItem.creator.hex || isPartner,
				incentivePlan = nodeItem.incentivePlan
		)
	}


	fun covertVoteRecord(voteRecords: List<VoteRecordInfo>?, walletAddress: String): List<VoteRecordView>? {
		return voteRecords?.mapIndexed { index, voteRecordInfo ->
			VoteRecordView(
					index,
					voteRecordInfo.address,
					App.numberFormatter.formatCoinFull(valueConvert(voteRecordInfo.lockValue), "SAFE", 2),
					walletAddress.equals(voteRecordInfo.address, true)
			)
		}
	}

	fun convertLockIdItemView(list: List<LockIdsInfo>?): List<LockIdsView>? {
		return list?.distinctBy { it.lockId }?.sortedByDescending { it.lockId }
			?.mapIndexed { index, record ->
			val value = valueConvert(record.lockValue)
			LockIdsView(
					index,
					record.lockId.toString(),
					App.numberFormatter.formatCoinFull(value, "SAFE", 2),
					record.enable,
					record.checked
			)
		}?.sortedBy { it.enable }
	}

	fun convertCreatorList(nodeItem: NodeInfo?, walletAddress: String): List<CreateViewItem> {
		nodeItem ?: return emptyList()
		return nodeItem.founders.map {
			CreateViewItem(
					it.lockID.toString(),
					it.addr.hex,
					App.numberFormatter.formatCoinFull(valueConvert(it.amount), "SAFE", 2),
					it.addr.hex == walletAddress
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
		val agreeNum = voteRecord?.filter { it.state == 1 }?.size ?: 0
		val rejectNum = voteRecord?.filter { it.state == 2 }?.size ?: 0
		val abstentionNum = voteRecord?.filter { it.state == 3 }?.size ?: 0
		val status = if (info.state == 0 && info.endPayTime * 1000 < System.currentTimeMillis())
						ProposalStatus.getStatus(2)
					else
						ProposalStatus.getStatus(info.state)
		return ProposalInfoViewItem(
				info.id,
				info.title,
				info.description,
				info.creator,
				status,
				info.payTimes.toInt(),
				App.numberFormatter.formatCoinFull(valueConvert(info.payAmount), "SAFE", 2),
				DateFormatUtils.format(info.startPayTime * 1000, "yyyy-MM-dd HH:mm:ss"),
				DateFormatUtils.format(info.endPayTime * 1000, "yyyy-MM-dd HH:mm:ss"),
				agreeNum,
				rejectNum,
				abstentionNum
		)
	}


	fun covertSuperNode(info: SuperNodeInfo, walletAddress: Address): NodeInfo {
		return NodeInfo(
			info.id.toInt(),
			io.horizontalsystems.bankwallet.entities.Address(info.addr.value),
			io.horizontalsystems.bankwallet.entities.Address(info.creator.value),
			info.enode,
			info.description,
			info.isOfficial,
			if (info.state.toInt() == 2) NodeStatus.Exception else NodeStatus.Online,
			info.founders.map {
				NodeMemberInfo(it.lockID.toLong(), io.horizontalsystems.bankwallet.entities.Address(it.addr.value), it.amount, it.unlockHeight.toLong())
			},
			NodeIncentivePlan(info.incentivePlan.creator.toInt(), info.incentivePlan.partner.toInt(), info.incentivePlan.voter.toInt()),
			info.lastRewardHeight.toLong(),
			info.createHeight.toLong(),
			info.updateHeight.toLong(),
			info.name,
			availableLimit = NodeCovertFactory.scaleConvert(Super_Node_Create_Amount) - info.founders.sumOf { it.amount },
			isEdit = walletAddress.hex.equals(info.creator.value, true)

		)
	}

	fun covertMasterNode(info: MasterNodeInfo, walletAddress: Address): NodeInfo {
		return NodeInfo(
			info.id.toInt(),
			io.horizontalsystems.bankwallet.entities.Address(info.addr.value),
			io.horizontalsystems.bankwallet.entities.Address(info.creator.value),
			info.enode,
			info.description,
			info.isOfficial,
			if (info.state.toInt() == 2) NodeStatus.Exception else NodeStatus.Online ,
			info.founders.map {
				NodeMemberInfo(it.lockID.toLong(), io.horizontalsystems.bankwallet.entities.Address(it.addr.value), it.amount, it.unlockHeight.toLong())
			},
			NodeIncentivePlan(info.incentivePlan.creator.toInt(), info.incentivePlan.partner.toInt(), info.incentivePlan.voter.toInt()),
			info.lastRewardHeight.toLong(),
			info.createHeight.toLong(),
			info.updateHeight.toLong(),
			availableLimit = NodeCovertFactory.scaleConvert(Master_Node_Create_Amount) - info.founders.sumOf { it.amount },
			isEdit = walletAddress.hex.equals(info.creator.value, true)
		)
	}

	fun valueConvert(value: BigInteger, n: Int = 18): BigDecimal {
		return value.toBigDecimal().movePointLeft(n).stripTrailingZeros()
	}

	fun scaleConvert(value: Int): BigInteger {
		return value.toBigDecimal().movePointRight(18).stripTrailingZeros().toBigInteger()
	}

	fun scaleConvert(value: BigDecimal): BigInteger {
		return value.movePointRight(18).stripTrailingZeros().toBigInteger()
	}

	fun formatSafe(value: BigInteger, n:Int = 18, code: String = "SAFE"): String {
		val decimal = valueConvert(value, n)
		return App.numberFormatter.formatCoinFull(decimal, code, 8)
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