#ifndef CDX_LIST_H
#define CDX_LIST_H

#define CDX_LIST_POISON1  ((void *) 0x00100100) 
#define CDX_LIST_POISON2  ((void *) 0x00200200) 

typedef struct CdxListNodeS CdxListNodeT;
typedef struct CdxListS CdxListT;

struct CdxListNodeS
{
    struct CdxListNodeS *next;
    struct CdxListNodeS *prev;
};

struct CdxListS
{
    struct CdxListNodeS *head;
    struct CdxListNodeS *tail;
};

/*
 * Simple doubly linked list implementation.
 *
 * Some of the internal functions ("__xxx") are useful when
 * manipulating whole lists rather than single entries, as
 * sometimes we already know the next/prev entries and we can
 * generate better code by using them directly rather than
 * using the generic single-entry routines.
 */
#define CdxListInit(list) do { \
    (list)->head = (list)->tail = (struct CdxListNodeS *)(list);\
    }while (0)
    
#define CdxListNodeInit(node) do { \
    (node)->next = (node)->prev = (node);\
    }while (0)

/*
 * Insert a new entry between two known consecutive entries.
 *
 * This is only for internal list manipulation where we know
 * the prev/next entries already!
 */
static inline void __CdxListAdd(struct CdxListNodeS *new,
                        struct CdxListNodeS *prev, struct CdxListNodeS *next)
{
	next->prev = new;
	new->next = next;
	new->prev = prev;
	prev->next = new;
}

/**
 * list_add - add a new entry
 * @new: new entry to be added
 * @head: list head to add it after
 *
 * Insert a new entry after the specified head.
 * This is good for implementing stacks.
 */
static inline void CdxListAdd(struct CdxListNodeS *new, struct CdxListS *list)
{
	__CdxListAdd(new, (struct CdxListNodeS *)list, list->head);
}

static inline void CdxListAddBefore(struct CdxListNodeS *new, 
                                    struct CdxListNodeS *pos)
{
	__CdxListAdd(new, pos->prev, pos);
}

static inline void CdxListAddAfter(struct CdxListNodeS *new, 
                                    struct CdxListNodeS *pos)
{
	__CdxListAdd(new, pos, pos->next);
}

static inline void CdxListAddTail(struct CdxListNodeS *new, struct CdxListS *list)
{
	__CdxListAdd(new, list->tail, (struct CdxListNodeS *)list);
}

/*
 * Delete a list entry by making the prev/next entries
 * point to each other.
 *
 * This is only for internal list manipulation where we know
 * the prev/next entries already!
 */
static inline void __CdxListDel(struct CdxListNodeS *prev, struct CdxListNodeS *next)
{
	next->prev = prev;
	prev->next = next;
}

/**
 * list_del - deletes entry from list.
 * @entry: the element to delete from the list.
 * Note: list_empty() on entry does not return true after this, the entry is
 * in an undefined state.
 */
static inline void __CdxListDelEntry(struct CdxListNodeS *node)
{
	__CdxListDel(node->prev, node->next);
}

static inline void CdxListDel(struct CdxListNodeS *node)
{
	__CdxListDel(node->prev, node->next);
	node->next = CDX_LIST_POISON1;
	node->prev = CDX_LIST_POISON2;
}

/**
 * list_replace - replace old entry by new one
 * @old : the element to be replaced
 * @new : the new element to insert
 *
 * If @old was empty, it will be overwritten.
 */
static inline void CdxListReplace(struct CdxListNodeS *old,
				                struct CdxListNodeS *new)
{
	new->next = old->next;
	new->next->prev = new;
	new->prev = old->prev;
	new->prev->next = new;
}

static inline void CdxListReplaceInit(struct CdxListNodeS *old,
					            struct CdxListNodeS *new)
{
	CdxListReplace(old, new);
	CdxListNodeInit(old);
}

/**
 * list_del_init - deletes entry from list and reinitialize it.
 * @entry: the element to delete from the list.
 */
static inline void CdxListDelInit(struct CdxListNodeS *node)
{
	__CdxListDelEntry(node);
	CdxListNodeInit(node);
}

/**
 * list_move - delete from one list and add as another's head
 * @list: the entry to move
 * @head: the head that will precede our entry
 */
static inline void CdxListMove(struct CdxListNodeS *node, struct CdxListS *list)
{
	__CdxListDelEntry(node);
	CdxListAdd(node, list);
}

/**
 * list_move_tail - delete from one list and add as another's tail
 * @list: the entry to move
 * @head: the head that will follow our entry
 */
static inline void CdxListMoveTail(struct CdxListNodeS *node, 
                                struct CdxListS *list)
{
	__CdxListDelEntry(node);
	CdxListAddTail(node, list);
}

/**
 * list_is_last - tests whether @list is the last entry in list @head
 * @list: the entry to test
 * @head: the head of the list
 */
static inline int CdxListIsLast(const struct CdxListNodeS *node,
				            const struct CdxListS *list)
{
	return node->next == (struct CdxListNodeS *)list;
}

/**
 * list_empty - tests whether a list is empty
 * @head: the list to test.
 */
static inline int CdxListEmpty(const struct CdxListS *list)
{
	return (list->head == (struct CdxListNodeS *)list) 
	       && (list->tail == (struct CdxListNodeS *)list);
}

/**
 * list_rotate_left - rotate the list to the left
 * @head: the head of the list
 */
static inline void CdxListRotateLeft(struct CdxListS *list)
{
	struct CdxListNodeS *first;

	if (!CdxListEmpty(list)) {
		first = list->head;
		CdxListMoveTail(first, list);
	}
}

/**
 * list_is_singular - tests whether a list has just one entry.
 * @head: the list to test.
 */
static inline int CdxListIsSingular(const struct CdxListS *list)
{
	return !CdxListEmpty(list) && (list->head == list->tail);
}

/**
 * list_entry - get the struct for this entry
 * @ptr:	the &struct list_head pointer.
 * @type:	the type of the struct this is embedded in.
 * @member:	the name of the list_struct within the struct.
 */
#define CdxListEntry(ptr, type, member) \
	CdxContainerOf(ptr, type, member)

/**
 * list_first_entry - get the first element from a list
 * @ptr:	the list head to take the element from.
 * @type:	the type of the struct this is embedded in.
 * @member:	the name of the list_struct within the struct.
 *
 * Note, that list is expected to be not empty.
 */
#define CdxListFirstEntry(ptr, type, member) \
	CdxListEntry((ptr)->head, type, member)

/**
 * list_for_each	-	iterate over a list
 * @pos:	the &struct list_head to use as a loop cursor.
 * @head:	the head for your list.
 */
#define CdxListForEach(pos, list) \
	for (pos = (list)->head; \
	        pos != (struct CdxListNodeS *)(list);\
	        pos = pos->next)

/**
 * list_for_each_prev	-	iterate over a list backwards
 * @pos:	the &struct list_head to use as a loop cursor.
 * @head:	the head for your list.
 */
#define CdxListForEachPrev(pos, list) \
	for (pos = (list)->tail; \
	    pos != (struct CdxListNodeS *)(list); \
	    pos = pos->prev)

/**
 * list_for_each_safe - iterate over a list safe against removal of list entry
 * @pos:	the &struct list_head to use as a loop cursor.
 * @n:		another &struct list_head to use as temporary storage
 * @head:	the head for your list.
 */
#define CdxListForEachSafe(pos, n, list) \
	for (pos = (list)->head, n = pos->next; \
	    pos != (struct CdxListNodeS *)(list); \
		pos = n, n = pos->next)

/**
 * list_for_each_prev_safe - iterate over a list backwards safe against removal of list entry
 * @pos:	the &struct list_head to use as a loop cursor.
 * @n:		another &struct list_head to use as temporary storage
 * @head:	the head for your list.
 */
#define CdxListForEachPrevSafe(pos, n, list) \
	for (pos = (list)->tail, n = pos->prev; \
	     pos != (struct CdxListNodeS *)(list); \
	     pos = n, n = pos->prev)

/**
 * list_for_each_entry	-	iterate over list of given type
 * @pos:	the type * to use as a loop cursor.
 * @head:	the head for your list.
 * @member:	the name of the list_struct within the struct.
 */
#define CdxListForEachEntry(pos, list, member)				\
	for (pos = CdxListEntry((list)->head, typeof(*pos), member);	\
	     &pos->member != (struct CdxListNodeS *)(list); 	\
	     pos = CdxListEntry(pos->member.next, typeof(*pos), member))

/**
 * list_for_each_entry_reverse - iterate backwards over list of given type.
 * @pos:	the type * to use as a loop cursor.
 * @head:	the head for your list.
 * @member:	the name of the list_struct within the struct.
 */
#define CdxListForEachEntryReverse(pos, list, member)			\
	for (pos = CdxListEntry((list)->tail, typeof(*pos), member);	\
	     &pos->member != (struct CdxListNodeS *)(list); 	\
	     pos = CdxListEntry(pos->member.prev, typeof(*pos), member))

/**
 * list_for_each_entry_safe - iterate over list of given type safe against removal of list entry
 * @pos:	the type * to use as a loop cursor.
 * @n:		another type * to use as temporary storage
 * @head:	the head for your list.
 * @member:	the name of the list_struct within the struct.
 */
#define CdxListForEachEntrySafe(pos, n, list, member)			\
	for (pos = CdxListEntry((list)->head, typeof(*pos), member),	\
		n = CdxListEntry(pos->member.next, typeof(*pos), member);	\
	     &pos->member != (struct CdxListNodeS *)(list); 					\
	     pos = n, n = CdxListEntry(n->member.next, typeof(*n), member))

/**
 * list_for_each_entry_safe_reverse - iterate backwards over list safe against removal
 * @pos:	the type * to use as a loop cursor.
 * @n:		another type * to use as temporary storage
 * @head:	the head for your list.
 * @member:	the name of the list_struct within the struct.
 *
 * Iterate backwards over list of given type, safe against removal
 * of list entry.
 */
#define CdxListForEachEntrySafeReverse(pos, n, list, member)		\
	for (pos = CdxListEntry((list)->prev, typeof(*pos), member),	\
		n = CdxListEntry(pos->member.prev, typeof(*pos), member);	\
	     &pos->member != (struct CdxListNodeS *)(list); 					\
	     pos = n, n = CdxListEntry(n->member.prev, typeof(*n), member))

#endif
