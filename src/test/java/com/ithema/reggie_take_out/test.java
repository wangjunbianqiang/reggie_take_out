package com.ithema.reggie_take_out;

import java.util.*;

public class test {
    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);
        int n = in.nextInt();
        int k = in.nextInt();
        int[] nums = new int[n];
        for(int i = 0;i < n;i ++){
            nums[i] = in.nextInt();
        }
        int sum = 0;
        for (int i = 0; i < nums.length; i++) sum += nums[i];
        if (sum % k != 0) System.out.println(false);
        int target = sum / k;
        // 排序优化
        Arrays.sort(nums);
        int l = 0, r = nums.length - 1;
        while (l <= r) {
            int temp = nums[l];
            nums[l] = nums[r];
            nums[r] = temp;
            l++;
            r--;
        }
        System.out.println(backtrack(nums, 0, new int[k], k, target));
    }
    private static boolean backtrack(int[] nums, int index, int[] bucket, int k, int target) {
        // 结束条件优化
        if (index == nums.length) {
            return true;
        }
        for (int i = 0; i < k; i++) { //桶的数量
            // 优化点二
            if (i > 0 && bucket[i] == bucket[i - 1]) continue;
            // 剪枝
            if (bucket[i] + nums[index] > target) continue;
            bucket[i] += nums[index];
            //选择下一个球
            if (backtrack(nums, index + 1, bucket, k, target) == true) {
                return true;
            }
            //回溯
            bucket[i] -= nums[index];
        }
        return false;
    }
}
