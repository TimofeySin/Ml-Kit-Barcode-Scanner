package com.example.mlkitsample

import android.view.LayoutInflater

class Adapter1C {
//    class MyViewHolder(itemView: View): RecyclerView.ViewHolder(itemView){
//        val image: ImageView = itemView.image_movie
//        val txt_name: TextView = itemView.txt_name
//        val txt_team: TextView = itemView.txt_team
//        val txt_createdby: TextView = itemView.txt_createdby
//
//        fun bind(listItem: Movie) {
//            image.setOnClickListener {
//                Toast.makeText(it.context, "нажал на ${itemView.image_movie}", Toast.LENGTH_SHORT)
//                    .show()
//            }
//            itemView.setOnClickListener {
//                Toast.makeText(it.context, "нажал на ${itemView.txt_name.text}", Toast.LENGTH_SHORT).show()
//            }
//        }
//    }
//
//    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
//        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.drop_list_item, parent, false)
//        return MyViewHolder(itemView)
//    }
//
//    override fun getItemCount() = movieList.size
//
//    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
//        val listItem = movieList[position]
//        holder.bind(listItem)
//
//        Picasso.get().load(movieList[position].imageurl).into(holder.image)
//        holder.txt_name.text = movieList[position].name
//        holder.txt_team.text = movieList[position].team
//        holder.txt_createdby.text = movieList[position].createdby
    //}
}