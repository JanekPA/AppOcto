package com.example.octopus

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

class ReservationPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> CheckAvailabilityFragment()
            1 -> MyReservationsFragment()
            else -> Fragment()
        }
    }
}
