package com.basicorganizer.planner.adapter

import android.content.Context
import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.basicorganizer.planner.R
import com.basicorganizer.planner.data.TodoItem

class TodoAdapter(
    private val context: Context,
    private var todos: List<TodoItem>,
    private val listener: OnTodoInteractionListener
) : RecyclerView.Adapter<TodoAdapter.ViewHolder>() {

    interface OnTodoInteractionListener {
        fun onTodoCheckedChanged(todo: TodoItem, isChecked: Boolean)
        fun onTodoLongClick(todo: TodoItem)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_todo, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val todo = todos[position]

        holder.cbTodo.setOnCheckedChangeListener(null)
        holder.cbTodo.isChecked = todo.isCompleted
        holder.tvTitle.text = todo.title

        if (todo.isCompleted) {
            holder.tvTitle.setTextColor(ContextCompat.getColor(context, R.color.todo_checked))
            holder.tvTitle.paintFlags = holder.tvTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        } else {
            holder.tvTitle.setTextColor(ContextCompat.getColor(context, R.color.todo_unchecked))
            holder.tvTitle.paintFlags = holder.tvTitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
        }

        holder.ivMoveIndicator.visibility = if (todo.moveToNext && !todo.isCompleted) View.VISIBLE else View.GONE

        holder.cbTodo.setOnCheckedChangeListener { _, isChecked ->
            listener.onTodoCheckedChanged(todo, isChecked)
        }

        holder.itemView.setOnLongClickListener {
            listener.onTodoLongClick(todo)
            true
        }
    }

    override fun getItemCount(): Int = todos.size

    fun updateTodos(newTodos: List<TodoItem>) {
        todos = newTodos
        notifyDataSetChanged()
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cbTodo: CheckBox = itemView.findViewById(R.id.cb_todo)
        val tvTitle: TextView = itemView.findViewById(R.id.tv_todo_title)
        val ivMoveIndicator: ImageView = itemView.findViewById(R.id.iv_move_indicator)
    }
}
