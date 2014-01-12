package com.offbynull.peernetic.overlay.chord.core;

import com.offbynull.peernetic.overlay.chord.core.RouteResult;
import com.offbynull.peernetic.overlay.chord.core.FingerTable;
import com.offbynull.peernetic.overlay.chord.core.RouteResult.ResultType;
import com.offbynull.peernetic.overlay.common.id.Id;
import com.offbynull.peernetic.overlay.common.id.Pointer;
import java.net.InetSocketAddress;
import org.junit.After;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;

public class FingerTableTest {
    
    public FingerTableTest() {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }

    @Test(expected=IllegalArgumentException.class)
    public void testPut1() { 
        Pointer<InetSocketAddress> basePtr = TestUtils.generatePointer(2, 0x00L);
        FingerTable ft = new FingerTable(basePtr);
        
        ft.put(basePtr);
    }
    
    @Test
    public void testPut2() { 
        Pointer<InetSocketAddress> basePtr = TestUtils.generatePointer(3, 0x00L);
        FingerTable ft = new FingerTable(basePtr);
        
        Pointer<InetSocketAddress> ptr4 = TestUtils.generatePointer(3, 0x04L);
        
        ft.put(ptr4);
        
        Pointer<InetSocketAddress> entry;
        
        entry = ft.get(0);
        assertEquals(ptr4, entry);
        entry = ft.get(1);
        assertEquals(ptr4, entry);
        entry = ft.get(2);
        assertEquals(ptr4, entry);
    }

    @Test
    public void testPut3() { 
        Pointer<InetSocketAddress> basePtr = TestUtils.generatePointer(3, 0x00L);
        FingerTable ft = new FingerTable(basePtr);
        
        Pointer<InetSocketAddress> ptr4 = TestUtils.generatePointer(3, 0x04L);
        Pointer<InetSocketAddress> ptr5 = TestUtils.generatePointer(3, 0x05L);
        
        ft.put(ptr4);
        ft.put(ptr5);
        
        Pointer<InetSocketAddress> entry;
        
        entry = ft.get(0);
        assertEquals(ptr4, entry);
        entry = ft.get(1);
        assertEquals(ptr4, entry);
        entry = ft.get(2);
        assertEquals(ptr5, entry);
    }
    
    @Test
    public void testReplace1() { 
        Pointer<InetSocketAddress> basePtr = TestUtils.generatePointer(3, 0x00L);
        FingerTable ft = new FingerTable(basePtr);
        
        Pointer<InetSocketAddress> ptr4 = TestUtils.generatePointer(3, 0x04L);
        
        ft.replace(ptr4);
        
        Pointer<InetSocketAddress> entry;
        
        entry = ft.get(0);
        assertEquals(ptr4, entry);
        entry = ft.get(1);
        assertEquals(ptr4, entry);
        entry = ft.get(2);
        assertEquals(ptr4, entry);
    }
    
    @Test
    public void testReplace2() { 
        Pointer<InetSocketAddress> basePtr = TestUtils.generatePointer(3, 0x00L);
        FingerTable ft = new FingerTable(basePtr);
        
        Pointer<InetSocketAddress> ptr4 = TestUtils.generatePointer(3, 0x04L);
        Pointer<InetSocketAddress> ptr5 = TestUtils.generatePointer(3, 0x05L);
        
        ft.put(ptr5);
        ft.replace(ptr4);
        
        Pointer<InetSocketAddress> entry;
        
        entry = ft.get(0);
        assertEquals(ptr4, entry);
        entry = ft.get(1);
        assertEquals(ptr4, entry);
        entry = ft.get(2);
        assertEquals(ptr4, entry);
    }

    @Test
    public void testReplace3() { 
        Pointer<InetSocketAddress> basePtr = TestUtils.generatePointer(3, 0x00L);
        FingerTable ft = new FingerTable(basePtr);
        
        Pointer<InetSocketAddress> ptr1 = TestUtils.generatePointer(3, 0x01L);
        Pointer<InetSocketAddress> ptr4 = TestUtils.generatePointer(3, 0x04L);
        Pointer<InetSocketAddress> ptr5 = TestUtils.generatePointer(3, 0x05L);
        
        ft.replace(ptr1);
        ft.replace(ptr5);
        ft.replace(ptr4);
        
        Pointer<InetSocketAddress> entry;
        
        entry = ft.get(0);
        assertEquals(ptr1, entry);
        entry = ft.get(1);
        assertEquals(ptr4, entry);
        entry = ft.get(2);
        assertEquals(ptr4, entry);
    }
    
    @Test
    public void testEmptyFingerTable() {
        Pointer<InetSocketAddress> basePtr = TestUtils.generatePointer(2, 0x00L);
        FingerTable ft = new FingerTable(basePtr);
        
        Id id1 = TestUtils.generateId(2, 0x00L);
        Id id2 = TestUtils.generateId(2, 0x01L);
        Id id3 = TestUtils.generateId(2, 0x02L);
        Id id4 = TestUtils.generateId(2, 0x03L);
        
        RouteResult res;
        
        res = ft.route(id1);
        assertEquals(basePtr.getId(), res.getPointer().getId());
        assertEquals(ResultType.SELF, res.getResultType());
        res = ft.route(id2);
        assertEquals(basePtr.getId(), res.getPointer().getId());
        assertEquals(ResultType.SELF, res.getResultType());
        res = ft.route(id3);
        assertEquals(basePtr.getId(), res.getPointer().getId());
        assertEquals(ResultType.SELF, res.getResultType());
        res = ft.route(id4);
        assertEquals(basePtr.getId(), res.getPointer().getId());
        assertEquals(ResultType.SELF, res.getResultType());
    }

    @Test
    public void testPartialFingerTable() {
        Pointer<InetSocketAddress> basePtr = TestUtils.generatePointer(2, 0x00L);
        FingerTable ft = new FingerTable(basePtr);
        
        Pointer<InetSocketAddress> firstNeighbour = TestUtils.generatePointer(2, 0x01L);
        Pointer<InetSocketAddress> secondNeighbour = TestUtils.generatePointer(2, 0x02L);
        ft.put(secondNeighbour);
        Pointer<InetSocketAddress> thirdNeighbour = TestUtils.generatePointer(2, 0x03L);
        
        RouteResult res;
        
        res = ft.route(basePtr.getId());
        assertEquals(basePtr.getId(), res.getPointer().getId());
        assertEquals(ResultType.SELF, res.getResultType());
        // 1st is less than successor (2nd), so it'll give back 2nd
        res = ft.route(firstNeighbour.getId());
        assertEquals(secondNeighbour.getId(), res.getPointer().getId());
        assertEquals(ResultType.FOUND, res.getResultType());
        // 2nd is successor, so it'll give back 2nd
        res = ft.route(secondNeighbour.getId());
        assertEquals(secondNeighbour.getId(), res.getPointer().getId());
        assertEquals(ResultType.FOUND, res.getResultType());
        // 3rd is past successor, so it'll give back the last finger table
        // entry that's < 3rd (not <=, just <), which is 2nd 
        res = ft.route(thirdNeighbour.getId());
        assertEquals(secondNeighbour.getId(), res.getPointer().getId());
        assertEquals(ResultType.CLOSEST_PREDECESSOR, res.getResultType());
    }
    
    @Test
    public void testFullFingerTable() {
        Pointer<InetSocketAddress> basePtr = TestUtils.generatePointer(2, 0x00L);
        FingerTable ft = new FingerTable(basePtr);

        Pointer<InetSocketAddress> firstNeighbour = TestUtils.generatePointer(2, 0x01L);
        ft.put(firstNeighbour);
        Pointer<InetSocketAddress> secondNeighbour = TestUtils.generatePointer(2, 0x02L);
        ft.put(secondNeighbour);
        Pointer<InetSocketAddress> thirdNeighbour = TestUtils.generatePointer(2, 0x03L);
        ft.put(thirdNeighbour);
        
        RouteResult res;
        
        res = ft.route(basePtr.getId());
        assertEquals(basePtr.getId(), res.getPointer().getId());
        assertEquals(ResultType.SELF, res.getResultType());
        // 1st is successor (1st), so it'll give back 1st
        res = ft.route(firstNeighbour.getId());
        assertEquals(firstNeighbour.getId(), res.getPointer().getId());
        assertEquals(ResultType.FOUND, res.getResultType());
        // 2nd should be directly in the finger table (just after successor --
        // value 10b), but since 2nd is past the successor, this will give us
        // back the entry just before it, which should be the succesor (1st)
        res = ft.route(secondNeighbour.getId());
        assertEquals(firstNeighbour.getId(), res.getPointer().getId());
        assertEquals(ResultType.CLOSEST_PREDECESSOR, res.getResultType());
        // 3rd should be directly in the finger table (just after successor --
        // actual id 11b, placed in finger table index 2 for desired id of 10b),
        // but since it is past the successor, this will give us back the finger
        // table entry just before it, which should be the succesor (1st)
        res = ft.route(thirdNeighbour.getId());
        assertEquals(firstNeighbour.getId(), res.getPointer().getId());
        assertEquals(ResultType.CLOSEST_PREDECESSOR, res.getResultType());
    }
    
    @Test
    public void testRemove1() { 
        Pointer<InetSocketAddress> basePtr = TestUtils.generatePointer(2, 0x02L);
        FingerTable ft = new FingerTable(basePtr);
        Pointer<InetSocketAddress> ptr2 = TestUtils.generatePointer(2, 0x00L);
        Pointer<InetSocketAddress> ptr3 = TestUtils.generatePointer(2, 0x01L);

        ft.put(ptr2);
        ft.remove(ptr3);

        // nothing should be removed
        Pointer<InetSocketAddress> entry;
        
        entry = ft.get(1);
        assertEquals(ptr2, entry);
        entry = ft.get(0);
        assertEquals(ptr2, entry);
    }
    
    @Test
    public void testRemove2() { 
        Pointer<InetSocketAddress> basePtr = TestUtils.generatePointer(2, 0x02L);
        FingerTable ft = new FingerTable(basePtr);
        Pointer<InetSocketAddress> ptr2 = TestUtils.generatePointer(2, 0x00L);
        
        ft.put(ptr2);
        ft.remove(ptr2);

        // table should be reverted
        Pointer<InetSocketAddress> entry;
        
        entry = ft.get(0);
        assertEquals(basePtr, entry);
        entry = ft.get(1);
        assertEquals(basePtr, entry);
    }

    @Test
    public void testRemove3() {
        Pointer<InetSocketAddress> basePtr = TestUtils.generatePointer(3, 0x00L);
        FingerTable ft = new FingerTable(basePtr);
        Pointer<InetSocketAddress> ptr2 = TestUtils.generatePointer(3, 0x02L);
        Pointer<InetSocketAddress> ptr4 = TestUtils.generatePointer(3, 0x04L);

        ft.put(ptr2);
        ft.put(ptr4);
        ft.remove(ptr4);
        
        // preceding table entry should NOT be replaced
        Pointer<InetSocketAddress> entry;
        
        entry = ft.get(2);
        assertEquals(basePtr, entry);
        entry = ft.get(1);
        assertEquals(ptr2, entry);
        entry = ft.get(0);
        assertEquals(ptr2, entry);
    }

    @Test
    public void testClearBefore1() { 
        Pointer<InetSocketAddress> basePtr = TestUtils.generatePointer(3, 0x00L);
        FingerTable ft = new FingerTable(basePtr);
        Pointer<InetSocketAddress> ptr2 = TestUtils.generatePointer(3, 0x02L);
        Pointer<InetSocketAddress> ptr4 = TestUtils.generatePointer(3, 0x04L);
        
        ft.put(ptr2);
        ft.put(ptr4);
        int cleared = ft.clearBefore(ptr4.getId());

        assertEquals(1, cleared);
        // should be a clear finger table
        Pointer<InetSocketAddress> entry;
        
        entry = ft.get(2);
        assertEquals(ptr4, entry);
        entry = ft.get(1);
        assertEquals(ptr4, entry);
        entry = ft.get(0);
        assertEquals(ptr4, entry);
    }

    @Test
    public void testClearBefore2() { 
        Pointer<InetSocketAddress> basePtr = TestUtils.generatePointer(3, 0x00L);
        FingerTable ft = new FingerTable(basePtr);
        Pointer<InetSocketAddress> ptr2 = TestUtils.generatePointer(3, 0x02L);
        Pointer<InetSocketAddress> ptr3 = TestUtils.generatePointer(3, 0x03L);
        Pointer<InetSocketAddress> ptr4 = TestUtils.generatePointer(3, 0x04L);
        
        ft.put(ptr2);
        ft.put(ptr4);
        int cleared = ft.clearBefore(ptr3.getId());

        assertEquals(1, cleared);
        
        // should be a clear finger table
        Pointer<InetSocketAddress> entry;
        
        entry = ft.get(2);
        assertEquals(ptr4, entry);
        entry = ft.get(1);
        assertEquals(ptr4, entry);
        entry = ft.get(0);
        assertEquals(ptr4, entry);
    }

    @Test
    public void testClearBefore3() { 
        Pointer<InetSocketAddress> basePtr = TestUtils.generatePointer(3, 0x00L);
        FingerTable ft = new FingerTable(basePtr);
        Pointer<InetSocketAddress> ptr1 = TestUtils.generatePointer(3, 0x01L);
        Pointer<InetSocketAddress> ptr2 = TestUtils.generatePointer(3, 0x02L);
        Pointer<InetSocketAddress> ptr4 = TestUtils.generatePointer(3, 0x04L);
        
        ft.put(ptr2);
        ft.put(ptr4);
        int cleared = ft.clearBefore(ptr1.getId());
        
        assertEquals(0, cleared);

        // should be a clear finger table
        Pointer<InetSocketAddress> entry;
        
        entry = ft.get(2);
        assertEquals(ptr4, entry);
        entry = ft.get(1);
        assertEquals(ptr2, entry);
        entry = ft.get(0);
        assertEquals(ptr2, entry);
    }

    @Test
    public void testClearAfter1() { 
        Pointer<InetSocketAddress> basePtr = TestUtils.generatePointer(3, 0x00L);
        FingerTable ft = new FingerTable(basePtr);
        Pointer<InetSocketAddress> ptr2 = TestUtils.generatePointer(3, 0x02L);
        Pointer<InetSocketAddress> ptr4 = TestUtils.generatePointer(3, 0x04L);
        
        ft.put(ptr2);
        ft.put(ptr4);
        int cleared = ft.clearAfter(basePtr.getId());
        
        assertEquals(3, cleared);

        // should be a clear finger table
        Pointer<InetSocketAddress> entry;
        
        entry = ft.get(2);
        assertEquals(basePtr, entry);
        entry = ft.get(1);
        assertEquals(basePtr, entry);
        entry = ft.get(0);
        assertEquals(basePtr, entry);
    }

    @Test
    public void testClearAfter2() { 
        Pointer<InetSocketAddress> basePtr = TestUtils.generatePointer(3, 0x00L);
        FingerTable ft = new FingerTable(basePtr);
        Pointer<InetSocketAddress> ptr2 = TestUtils.generatePointer(3, 0x02L);
        Pointer<InetSocketAddress> ptr3 = TestUtils.generatePointer(3, 0x03L);
        Pointer<InetSocketAddress> ptr4 = TestUtils.generatePointer(3, 0x04L);
        
        ft.put(ptr2);
        ft.put(ptr4);
        int cleared = ft.clearAfter(ptr3.getId());
        
        assertEquals(1, cleared);

        // should be a clear finger table
        Pointer<InetSocketAddress> entry;
        
        entry = ft.get(2);
        assertEquals(basePtr, entry);
        entry = ft.get(1);
        assertEquals(ptr2, entry);
        entry = ft.get(0);
        assertEquals(ptr2, entry);
    }

    @Test
    public void testClearAfter3() { 
        Pointer<InetSocketAddress> basePtr = TestUtils.generatePointer(3, 0x00L);
        FingerTable ft = new FingerTable(basePtr);
        Pointer<InetSocketAddress> ptr2 = TestUtils.generatePointer(3, 0x02L);
        Pointer<InetSocketAddress> ptr4 = TestUtils.generatePointer(3, 0x04L);
        Pointer<InetSocketAddress> ptr5 = TestUtils.generatePointer(3, 0x05L);
        
        ft.put(ptr2);
        ft.put(ptr4);
        int cleared = ft.clearAfter(ptr5.getId());
        
        assertEquals(0, cleared);

        // should be a clear finger table
        Pointer<InetSocketAddress> entry;
        
        entry = ft.get(2);
        assertEquals(ptr4, entry);
        entry = ft.get(1);
        assertEquals(ptr2, entry);
        entry = ft.get(0);
        assertEquals(ptr2, entry);
    }
}