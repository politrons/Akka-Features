package persistence.events

import persistence.Item

/**
  * Created by pabloperezgarcia on 03/02/2017.
  */
case class ItemAdded(item: Item) extends ItemEvent