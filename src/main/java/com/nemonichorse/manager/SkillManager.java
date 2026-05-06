package com.nemonichorse.manager;

import com.nemonichorse.skill.*;

import java.util.*;

public final class SkillManager {

    private final Map<String, HorseSkill> skills = new LinkedHashMap<>();

    public SkillManager() {
        register(new DashSkill());
        register(new JumpBoostSkill());
        register(new IronHideSkill());
        register(new TrampleSkill());
    }

    public void register(HorseSkill skill) {
        skills.put(skill.getId(), skill);
    }

    public HorseSkill getSkill(String id) {
        return id == null ? null : skills.get(id.toUpperCase());
    }

    /** All skills in registration order. */
    public Collection<HorseSkill> getAllSkills() {
        return Collections.unmodifiableCollection(skills.values());
    }

    public boolean exists(String id) {
        return id != null && skills.containsKey(id.toUpperCase());
    }
}
