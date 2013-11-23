package lisong_mechlab.model.item;

import lisong_mechlab.model.chassi.HardpointType;
import lisong_mechlab.model.loadout.Loadout;
import lisong_mechlab.model.loadout.Upgrades;
import lisong_mechlab.model.mwo_parsing.helpers.ItemStatsWeapon;

import java.util.Comparator;

public class Weapon extends HeatSource{
   public static final int RANGE_ULP_FUZZ = 5;

   protected final double  damagePerProjectile;
   protected final double  cycleTime;
   protected final double  rangeMin;
   protected final double  rangeLong;
   protected final double  rangeMax;
   protected final int     ammoPerShot;
   protected final int     projectilesPerShot;

   protected final int     shotsPerFiring;

   public Weapon(ItemStatsWeapon aStatsWeapon, HardpointType aHardpointType){
      super(aStatsWeapon, aHardpointType, aStatsWeapon.WeaponStats.slots, aStatsWeapon.WeaponStats.tons, aStatsWeapon.WeaponStats.heat);
      damagePerProjectile = aStatsWeapon.WeaponStats.damage;
      cycleTime = aStatsWeapon.WeaponStats.cooldown;
      rangeMin = aStatsWeapon.WeaponStats.minRange;
      rangeMax = aStatsWeapon.WeaponStats.maxRange;
      rangeLong = aStatsWeapon.WeaponStats.longRange;

      shotsPerFiring = aStatsWeapon.WeaponStats.numFiring;
      projectilesPerShot = aStatsWeapon.WeaponStats.numPerShot > 0 ? aStatsWeapon.WeaponStats.numPerShot : 1;
      ammoPerShot = aStatsWeapon.WeaponStats.ammoPerShot;
   }

   public double getDamagePerShot(){
      return damagePerProjectile * projectilesPerShot * shotsPerFiring;
   }

   public int getAmmoPerPerShot(){
      return ammoPerShot;
   }

   public double getSecondsPerShot(){
      if( cycleTime < 0.1 )
         return 0.10375; // Determined on testing grounds: 4000 mg rounds 6min 55s or 415s -> 415/4000 = 0.10375
      return cycleTime;
   }

   public double getRangeZero(){
      return 0;
   }

   public double getRangeMin(){
      return rangeMin;
   }

   public double getRangeMax(){
      return rangeMax;
   }

   public double getRangeLong(){
      return rangeLong;
   }

   public double getRangeEffectivity(double range){
      // Assume linear fall off
      if( range < getRangeZero() )
         return 0;
      if( range < getRangeMin() )
         return (range - getRangeZero()) / (getRangeMin() - getRangeZero());
      else if( range <= getRangeLong() )
         return 1.0;
      else if( range < getRangeMax() )
         return 1.0 - (range - getRangeLong()) / (getRangeMax() - getRangeLong());
      else
         return 0;
   }

   /**
    * Calculates an arbitrary statistic for the weapon based on the string. The string format is (regexp):
    * "[dsthc]+(/[dsthc]+)?" where d=damage, s=seconds, t=tons, h=heat, c=criticalslots. For example "d/hhs" is damage
    * per heat^2 second.
    * 
    * @param aWeaponStat
    *           A string specifying the statistic to be calculated. Must match the regexp pattern
    *           "[dsthc]+(/[dsthc]+)?".
    * @return The calculated statistic.
    */
   public double getStat(String aWeaponStat, Upgrades anUpgrades){
      double nominator = 1;
      int index = 0;
      while( index < aWeaponStat.length() && aWeaponStat.charAt(index) != '/' ){
         switch( aWeaponStat.charAt(index) ){
            case 'd':
               nominator *= getDamagePerShot();
               break;
            case 's':
               nominator *= getSecondsPerShot();
               break;
            case 't':
               nominator *= getMass(anUpgrades);
               break;
            case 'h':
               nominator *= getHeat();
               break;
            case 'c':
               nominator *= getNumCriticalSlots(anUpgrades);
               break;
            default:
               throw new IllegalArgumentException("Unknown identifier: " + aWeaponStat.charAt(index));
         }
         index++;
      }

      index++; // Skip past the '/' if we encountered it, otherwise we'll be at the end of the string anyway.
      double denominator = 1;
      while( index < aWeaponStat.length() ){
         switch( aWeaponStat.charAt(index) ){
            case 'd':
               denominator *= getDamagePerShot();
               break;
            case 's':
               denominator *= getSecondsPerShot();
               break;
            case 't':
               denominator *= getMass(anUpgrades);
               break;
            case 'h':
               denominator *= getHeat();
               break;
            case 'c':
               denominator *= getNumCriticalSlots(anUpgrades);
               break;
            default:
               throw new IllegalArgumentException("Unknown identifier: " + aWeaponStat.charAt(index));
         }
         index++;
      }
      if(nominator == 0.0 && denominator == 0.0){
         // We take the Brahmaguptan interpretation of 0/0 to be 0 (year 628). 
         return 0;
      }
      return nominator / denominator;
   }

   @Override
   public boolean isEquippableOn(Loadout aLoadout){
      return aLoadout.getChassi().getHardpointsCount(getHardpointType()) > 0;
   }



    /**
     * Should compare the Weapon first by the Type then by damage output.
     */
     public static final Comparator<Weapon> WEAPON_TYPE_DAMAGE_ORDER = new Comparator<Weapon>() {
         @Override
         public int compare(Weapon o1, Weapon o2) {
             int comparedClassNames = o1.getClass().getName().compareTo(o2.getClass().getName());
             Double o1DPShot = o1.getDamagePerShot();
             Double o2DPShot = o2.getDamagePerShot();

             if(comparedClassNames != 0){
                 return comparedClassNames;
             }
             return (o1DPShot < o2DPShot) ? -1 : (o1DPShot == o2DPShot) ? 0 : 1;
         }
     };
}
