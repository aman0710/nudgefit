-- Update old activity levels to new unified terminology
UPDATE users SET activity_level = 'MODERATE' WHERE activity_level IN ('LIGHTLY_ACTIVE', 'MODERATELY_ACTIVE');
UPDATE users SET activity_level = 'ACTIVE' WHERE activity_level = 'VERY_ACTIVE';
