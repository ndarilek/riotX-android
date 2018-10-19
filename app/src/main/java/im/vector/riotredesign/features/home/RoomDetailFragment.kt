package im.vector.riotredesign.features.home

import android.arch.lifecycle.Observer
import android.arch.paging.PagedList
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import im.vector.matrix.android.api.Matrix
import im.vector.matrix.android.api.session.events.model.Event
import im.vector.matrix.android.api.session.room.Room
import im.vector.riotredesign.R
import im.vector.riotredesign.core.platform.RiotFragment
import im.vector.riotredesign.core.utils.FragmentArgumentDelegate
import kotlinx.android.synthetic.main.fragment_room_list.*
import org.koin.android.ext.android.inject

class RoomDetailFragment : RiotFragment() {

    companion object {

        fun newInstance(roomId: String): RoomDetailFragment {
            return RoomDetailFragment().apply {
                this.roomId = roomId
            }
        }
    }

    private val matrix by inject<Matrix>()
    private val currentSession = matrix.currentSession!!
    private var roomId by FragmentArgumentDelegate<String>()
    private val timelineController = TimelineEventController()
    private lateinit var room: Room

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_room_detail, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setupRecyclerView()
        room = currentSession.getRoom(roomId)!!
        room.liveTimeline().observe(this, Observer { renderEvents(it) })
    }

    private fun renderEvents(events: PagedList<Event>?) {
        timelineController.submitList(events)
    }

    private fun setupRecyclerView() {
        val linearLayoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        linearLayoutManager.stackFromEnd = true
        epoxyRecyclerView.layoutManager = linearLayoutManager
        epoxyRecyclerView.setController(timelineController)
    }


}