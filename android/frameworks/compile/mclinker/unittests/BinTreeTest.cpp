//===- BinTreeTest.cpp ----------------------------------------------------===//
//
//                     The MCLinker Project
//
// This file is distributed under the University of Illinois Open Source
// License. See LICENSE.TXT for details.
//
//===----------------------------------------------------------------------===//
#include "BinTreeTest.h"

#include "mcld/ADT/TypeTraits.h"
#include "mcld/MC/InputTree.h"
#include <string>

using namespace mcld;
using namespace mcldtest;


// Constructor can do set-up work for all test here.
BinTreeTest::BinTreeTest()
{
	// create testee. modify it if need
	m_pTestee = new BinaryTree<int>();
}

// Destructor can do clean-up work that doesn't throw exceptions here.
BinTreeTest::~BinTreeTest()
{
	delete m_pTestee;
}

// SetUp() will be called immediately before each test.
void BinTreeTest::SetUp()
{
}

// TearDown() will be called immediately after each test.
void BinTreeTest::TearDown()
{
}

//==========================================================================//
// Testcases
//


/// General 
TEST_F( BinTreeTest,Two_non_null_tree_merge) 
{
  BinaryTree<int>::iterator pos = m_pTestee->root();
  m_pTestee->join<TreeIteratorBase::Rightward>(pos,0);
  --pos;
  m_pTestee->join<TreeIteratorBase::Rightward>(pos,1);
  m_pTestee->join<TreeIteratorBase::Leftward>(pos,1);
  --pos;
  m_pTestee->join<TreeIteratorBase::Rightward>(pos,2);
  m_pTestee->join<TreeIteratorBase::Leftward>(pos,2);

  BinaryTree<int> *mergeTree = new BinaryTree<int>;
  BinaryTree<int>::iterator pos2 = mergeTree->root();
  mergeTree->join<TreeIteratorBase::Rightward>(pos2,1);
  --pos2;
  mergeTree->join<TreeIteratorBase::Rightward>(pos2,1);
  mergeTree->join<TreeIteratorBase::Leftward>(pos2,1);

  m_pTestee->merge<TreeIteratorBase::Rightward>(pos,*mergeTree); 
  delete mergeTree;
  EXPECT_TRUE(m_pTestee->size()==8);
}

/// ---- TEST - 2 ----
TEST_F( BinTreeTest, A_null_tree_merge_a_non_null_tree) 
{ 
  BinaryTree<int>::iterator pos = m_pTestee->root();
 
  BinaryTree<int> *mergeTree = new BinaryTree<int>;
  mergeTree->join<TreeIteratorBase::Rightward>(pos,0);
  --pos;
  mergeTree->join<TreeIteratorBase::Rightward>(pos,1);
  mergeTree->join<TreeIteratorBase::Leftward>(pos,1);
  --pos;
  mergeTree->join<TreeIteratorBase::Rightward>(pos,2);
  mergeTree->join<TreeIteratorBase::Leftward>(pos,2);

  m_pTestee->merge<TreeIteratorBase::Rightward>(pos,*mergeTree); 

  delete mergeTree;
  EXPECT_TRUE(m_pTestee->size()==5);
}

TEST_F( BinTreeTest, A_non_null_tree_merge_a_null_tree) 
{ 
  BinaryTree<int>::iterator pos = m_pTestee->root();
  m_pTestee->join<TreeIteratorBase::Rightward>(pos,0);
  --pos;
  m_pTestee->join<TreeIteratorBase::Rightward>(pos,1);
  m_pTestee->join<TreeIteratorBase::Leftward>(pos,1);
  --pos;
  m_pTestee->join<TreeIteratorBase::Rightward>(pos,2);
  m_pTestee->join<TreeIteratorBase::Leftward>(pos,2);
  
  BinaryTree<int> *mergeTree = new BinaryTree<int>;
  BinaryTree<int>::iterator pos2 = mergeTree->root(); 
  mergeTree->merge<TreeIteratorBase::Rightward>(pos2,*m_pTestee); 

  //delete m_pTestee;
  EXPECT_TRUE(mergeTree->size()==5);
  delete mergeTree;
}

TEST_F( BinTreeTest, Two_null_tree_merge) 
{ 
  BinaryTree<int>::iterator pos = m_pTestee->root();
 
  BinaryTree<int> *mergeTree = new BinaryTree<int>;
  BinaryTree<int>::iterator pos2 = mergeTree->root(); 

  mergeTree->merge<TreeIteratorBase::Rightward>(pos2,*m_pTestee); 

  //delete m_pTestee;
  EXPECT_TRUE(mergeTree->size()==0);
  delete mergeTree;
}

TEST_F( BinTreeTest, DFSIterator_BasicTraversal)
{
  int a = 111;
  BinaryTree<int>::iterator pos = m_pTestee->root();
  
  m_pTestee->join<InputTree::Inclusive>(pos,a);
  pos.move<InputTree::Inclusive>();
  m_pTestee->join<InputTree::Positional>(pos,10);
  m_pTestee->join<InputTree::Inclusive>(pos,9);
  pos.move<InputTree::Inclusive>();
  m_pTestee->join<InputTree::Positional>(pos,8);
  m_pTestee->join<InputTree::Inclusive>(pos,7);
  
  BinaryTree<int>::dfs_iterator dfs_it = m_pTestee->dfs_begin(); 
  BinaryTree<int>::dfs_iterator dfs_end = m_pTestee->dfs_end(); 

  ASSERT_EQ(111, **dfs_it);
  ++dfs_it;
  ASSERT_EQ(9, **dfs_it);
  ++dfs_it;
  ASSERT_EQ(7, **dfs_it);
  ++dfs_it;
  ASSERT_EQ(8, **dfs_it);
  ++dfs_it;
  ASSERT_EQ(10, **dfs_it);
  ++dfs_it;
  ASSERT_TRUE( dfs_it ==  dfs_end);
  BinaryTree<int>::bfs_iterator bfs_it = m_pTestee->bfs_begin(); 
  BinaryTree<int>::bfs_iterator bfs_end = m_pTestee->bfs_end(); 
}

TEST_F( BinTreeTest, DFSIterator_RightMostTree)
{
  BinaryTree<int>::iterator pos = m_pTestee->root();
  m_pTestee->join<InputTree::Inclusive>(pos,0);
  pos.move<InputTree::Inclusive>();
  m_pTestee->join<InputTree::Positional>(pos,1);
  pos.move<InputTree::Positional>();
  m_pTestee->join<InputTree::Positional>(pos,2);
  pos.move<InputTree::Positional>();
  m_pTestee->join<InputTree::Positional>(pos,3);
  pos.move<InputTree::Positional>();
  m_pTestee->join<InputTree::Positional>(pos,4);
  
  BinaryTree<int>::dfs_iterator dfs_it = m_pTestee->dfs_begin(); 
  BinaryTree<int>::dfs_iterator dfs_end = m_pTestee->dfs_end(); 

  ASSERT_EQ(0, **dfs_it);
  ++dfs_it;
  ASSERT_EQ(1, **dfs_it);
  ++dfs_it;
  ASSERT_EQ(2, **dfs_it);
  ++dfs_it;
  ASSERT_EQ(3, **dfs_it);
  ++dfs_it;
  ASSERT_EQ(4, **dfs_it);
  ++dfs_it;
  ASSERT_TRUE( dfs_it ==  dfs_end);
}


TEST_F( BinTreeTest, DFSIterator_SingleNode)
{
  BinaryTree<int>::iterator pos = m_pTestee->root();
  m_pTestee->join<InputTree::Inclusive>(pos,0);
  BinaryTree<int>::dfs_iterator dfs_it = m_pTestee->dfs_begin(); 
  BinaryTree<int>::dfs_iterator dfs_end = m_pTestee->dfs_end(); 
  int counter = 0;
  while( dfs_it != dfs_end ) {
    ++counter;
    ++dfs_it;
  }
  ASSERT_EQ(1, counter);
}

TEST_F( BinTreeTest, BFSIterator_BasicTraversal)
{
  int a = 111;
  BinaryTree<int>::iterator pos = m_pTestee->root();
  
  m_pTestee->join<InputTree::Inclusive>(pos,a);
  pos.move<InputTree::Inclusive>();
  m_pTestee->join<InputTree::Positional>(pos,10);
  m_pTestee->join<InputTree::Inclusive>(pos,9);
  pos.move<InputTree::Inclusive>();
  m_pTestee->join<InputTree::Positional>(pos,8);
  m_pTestee->join<InputTree::Inclusive>(pos,7);
  
  BinaryTree<int>::bfs_iterator bfs_it = m_pTestee->bfs_begin(); 
  BinaryTree<int>::bfs_iterator bfs_end = m_pTestee->bfs_end(); 

  ASSERT_EQ(111, **bfs_it);
  ++bfs_it;
  ASSERT_EQ(10, **bfs_it);
  ++bfs_it;
  ASSERT_EQ(9, **bfs_it);
  ++bfs_it;
  ASSERT_EQ(8, **bfs_it);
  ++bfs_it;
  ASSERT_EQ(7, **bfs_it);
  ++bfs_it;
  ASSERT_TRUE(bfs_it ==  bfs_end);
  bfs_it = m_pTestee->bfs_begin(); 
  bfs_end = m_pTestee->bfs_end(); 
}

TEST_F( BinTreeTest, BFSIterator_RightMostTree)
{
  BinaryTree<int>::iterator pos = m_pTestee->root();
  m_pTestee->join<InputTree::Inclusive>(pos,0);
  pos.move<InputTree::Inclusive>();
  m_pTestee->join<InputTree::Positional>(pos,1);
  pos.move<InputTree::Positional>();
  m_pTestee->join<InputTree::Positional>(pos,2);
  pos.move<InputTree::Positional>();
  m_pTestee->join<InputTree::Positional>(pos,3);
  pos.move<InputTree::Positional>();
  m_pTestee->join<InputTree::Positional>(pos,4);
  
  BinaryTree<int>::bfs_iterator bfs_it = m_pTestee->bfs_begin(); 
  BinaryTree<int>::bfs_iterator bfs_end = m_pTestee->bfs_end(); 

  ASSERT_EQ(0, **bfs_it);
  ++bfs_it;
  ASSERT_EQ(1, **bfs_it);
  ++bfs_it;
  ASSERT_EQ(2, **bfs_it);
  ++bfs_it;
  ASSERT_EQ(3, **bfs_it);
  ++bfs_it;
  ASSERT_EQ(4, **bfs_it);
  ++bfs_it;
  ASSERT_TRUE( bfs_it ==  bfs_end);
}


TEST_F( BinTreeTest, BFSIterator_SingleNode)
{
  BinaryTree<int>::iterator pos = m_pTestee->root();
  m_pTestee->join<InputTree::Inclusive>(pos,0);
  BinaryTree<int>::bfs_iterator bfs_it = m_pTestee->bfs_begin(); 
  BinaryTree<int>::bfs_iterator bfs_end = m_pTestee->bfs_end(); 
  int counter = 0;
  while( bfs_it != bfs_end ) {
    ++counter;
    ++bfs_it;
  }
  ASSERT_EQ(1, counter);
}


