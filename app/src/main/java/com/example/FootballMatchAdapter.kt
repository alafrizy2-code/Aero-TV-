package com.example

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.model.FootballMatch
import com.example.model.MatchStatus

class FootballMatchAdapter(
    private val context: Context,
    private var rawMatches: List<FootballMatch> = emptyList(),
    private val onMatchClick: ((FootballMatch) -> Unit)? = null
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_MATCH = 1
    }

    sealed class ScheduleRow {
        data class DateHeader(val dateFormatted: String) : ScheduleRow()
        data class MatchRow(val match: FootballMatch) : ScheduleRow()
    }

    private val rows = mutableListOf<ScheduleRow>()

    init {
        buildRows(rawMatches)
    }

    fun updateMatches(newMatches: List<FootballMatch>) {
        this.rawMatches = newMatches
        buildRows(newMatches)
        notifyDataSetChanged()
    }

    private fun buildRows(matches: List<FootballMatch>) {
        rows.clear()
        if (matches.isEmpty()) return

        // Kelompokkan match berdasarkan tanggal
        // Jika ada 2+ match di tanggal yang sama, tanggal ditulis 1x saja sebagai header
        val grouped = matches.groupBy { it.matchDateWib }
        for ((dateTitle, matchList) in grouped) {
            rows.add(ScheduleRow.DateHeader(dateTitle))
            for (m in matchList) {
                rows.add(ScheduleRow.MatchRow(m))
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (rows[position]) {
            is ScheduleRow.DateHeader -> TYPE_HEADER
            is ScheduleRow.MatchRow -> TYPE_MATCH
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_HEADER) {
            val view = inflater.inflate(R.layout.item_schedule_date_header, parent, false)
            DateHeaderViewHolder(view)
        } else {
            val view = inflater.inflate(R.layout.item_schedule_match, parent, false)
            MatchViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val row = rows[position]) {
            is ScheduleRow.DateHeader -> (holder as DateHeaderViewHolder).bind(row.dateFormatted)
            is ScheduleRow.MatchRow -> (holder as MatchViewHolder).bind(row.match, onMatchClick)
        }
    }

    override fun getItemCount(): Int = rows.size

    class DateHeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvDateHeader: TextView = itemView.findViewById(R.id.tvDateHeader)

        fun bind(dateFormatted: String) {
            tvDateHeader.text = dateFormatted
        }
    }

    class MatchViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivHomeLogo: ImageView = itemView.findViewById(R.id.ivHomeLogo)
        private val tvHomeAbbr: TextView = itemView.findViewById(R.id.tvHomeAbbr)
        private val tvAwayAbbr: TextView = itemView.findViewById(R.id.tvAwayAbbr)
        private val ivAwayLogo: ImageView = itemView.findViewById(R.id.ivAwayLogo)
        private val tvMatchTime: TextView = itemView.findViewById(R.id.tvMatchTime)

        fun bind(match: FootballMatch, onClick: ((FootballMatch) -> Unit)?) {
            // Singkatan 3 huruf kapital di tengah
            tvHomeAbbr.text = match.shortHomeTeam
            tvAwayAbbr.text = match.shortAwayTeam

            // Logo klub kiri & kanan 28px
            if (!match.homeTeamLogoUrl.isNullOrEmpty()) {
                ivHomeLogo.load(match.homeTeamLogoUrl) {
                    crossfade(true)
                    placeholder(R.drawable.ic_football)
                    error(R.drawable.ic_football)
                }
            } else {
                ivHomeLogo.setImageResource(R.drawable.ic_football)
            }

            if (!match.awayTeamLogoUrl.isNullOrEmpty()) {
                ivAwayLogo.load(match.awayTeamLogoUrl) {
                    crossfade(true)
                    placeholder(R.drawable.ic_football)
                    error(R.drawable.ic_football)
                }
            } else {
                ivAwayLogo.setImageResource(R.drawable.ic_football)
            }

            // Jam di sebelah kanan klub, format WIB, rata kanan sejajar
            when (match.status) {
                MatchStatus.LIVE -> {
                    val score = "${match.homeScore ?: 0} - ${match.awayScore ?: 0}"
                    tvMatchTime.text = "$score LIVE"
                    tvMatchTime.setTextColor(Color.parseColor("#EF4444"))
                }
                MatchStatus.FINISHED -> {
                    val score = "${match.homeScore ?: 0} - ${match.awayScore ?: 0}"
                    tvMatchTime.text = "$score FT"
                    tvMatchTime.setTextColor(Color.parseColor("#CBD5E1"))
                }
                else -> {
                    tvMatchTime.text = match.matchTimeWib
                    tvMatchTime.setTextColor(Color.WHITE)
                }
            }

            // Focus highlight
            itemView.onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
                if (hasFocus) {
                    v.animate().scaleX(1.04f).scaleY(1.04f).setDuration(150).start()
                    v.elevation = 8f
                } else {
                    v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(150).start()
                    v.elevation = 0f
                }
            }

            itemView.setOnClickListener {
                onClick?.invoke(match)
            }
        }
    }
}
