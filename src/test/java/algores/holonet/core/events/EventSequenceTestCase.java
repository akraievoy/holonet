/*
 Copyright 2011 Anton Kraievoy akraievoy@gmail.com
 This file is part of Holonet.

 Holonet is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Holonet is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Holonet. If not, see <http://www.gnu.org/licenses/>.
 */

package algores.holonet.core.events;

import junit.framework.TestCase;

import java.util.Arrays;

/**
 * Unit test for Sequence class.
 */
public class EventSequenceTestCase extends TestCase {
  public void testGenerateNextEvent() {
    final Event join = new EventNodeJoin();
    final Event leave = new EventNodeLeave();
    final Event stabilize = new EventNodeStabilize();

    final EventCompositeSequence sequence =
        new EventCompositeSequence(
            Arrays.asList(
                new Event<?>[]{
                    join, stabilize, leave
                }
            )
        );

    validateSequence(sequence, join, stabilize, leave);
  }

  public void testReset() {
    final Event join = new EventNodeJoin();
    final Event leave = new EventNodeLeave();
    final Event stabilize = new EventNodeStabilize();

    final EventCompositeSequence sequence = new EventCompositeSequence(
        Arrays.asList(
            new Event<?>[]{
                join, stabilize, leave
            }
        )
    );

    sequence.generateNextEvent();
    sequence.generateNextEvent();
    sequence.reset();

    validateSequence(sequence, join, stabilize, leave);
  }

  private void validateSequence(final EventCompositeSequence sequence, final Event first, final Event second, final Event third) {
    assertFalse(sequence.isExhausted());
    assertSame(first, sequence.generateNextEvent());

    assertFalse(sequence.isExhausted());
    assertSame(second, sequence.generateNextEvent());

    assertFalse(sequence.isExhausted());
    assertSame(third, sequence.generateNextEvent());

    assertTrue(sequence.isExhausted());
    try {
      sequence.generateNextEvent();
      fail("Should fail");
    } catch (Exception e) {
      assertEquals(java.util.NoSuchElementException.class, e.getClass());
    }
  }
}