package games.strategy.engine.random;

import games.strategy.util.IntegerMap;

/**
 * Gets random numbers from javas random number generators.
 */
public class PlainRandomSource implements IRandomSource {
  /**
   * Knowing the seed gives a player an advantage.
   * Do something a little more clever than current time.
   * which could potentially be guessed
   * If the execution path is different before the first random
   * call is made then the object will have a somewhat random
   * adress in the virtual machine, especially if
   * a lot of ui and networking objects are created
   * in response to semi random mouse motion etc.
   * if the excecution is always the same then
   * this may vary depending on the vm
   */
  public static long getSeed() {
    final Object seedObj = new Object();
    long seed = seedObj.hashCode(); // hash code is an int, 32 bits
    seed += System.currentTimeMillis();
    seed += System.nanoTime(); // seed with current time as well
    return seed;
  }

  // private static Random s_random;
  private static MersenneTwister s_random;

  @Override
  public synchronized int[] getRandom(final int max, final int count, final String annotation)
      throws IllegalArgumentException {
    if (count <= 0) {
      throw new IllegalArgumentException("count must be > 0, annotation:" + annotation);
    }
    final int[] numbers = new int[count];
    for (int i = 0; i < count; i++) {
      numbers[i] = getRandom(max, annotation);
    }
    return numbers;
  }

  @Override
  public synchronized int getRandom(final int max, final String annotation) throws IllegalArgumentException {
    if (s_random == null) {
      s_random = new MersenneTwister(getSeed());
    }
    return s_random.nextInt(max);
  }

  public static void main(final String[] args) {
    final IntegerMap<Integer> results = new IntegerMap<Integer>();
    // TODO: does this need to be updated to take data.getDiceSides() ?
    final int[] random = new PlainRandomSource().getRandom(6, 100000, "Test");
    for (final int element : random) {
      results.add(Integer.valueOf(element + 1), 1);
    }
    System.out.println(results);
  }
}
