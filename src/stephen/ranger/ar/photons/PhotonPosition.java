package stephen.ranger.ar.photons;

public class PhotonPosition implements Comparable {
   public final Integer index;
   public final Float distanceSquared;

   public PhotonPosition(final Integer index, final Float distanceSquared) {
      this.index = index;
      this.distanceSquared = distanceSquared;
   }

   @Override
   public int compareTo(final Object o) {
      return this.distanceSquared.compareTo(((PhotonPosition) o).distanceSquared);
   }

}
