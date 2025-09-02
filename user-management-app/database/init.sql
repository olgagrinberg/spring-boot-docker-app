-- Create users table
CREATE TABLE IF NOT EXISTS users (
                                   id SERIAL PRIMARY KEY,
                                   name VARCHAR(255) NOT NULL,
  email VARCHAR(255) NOT NULL UNIQUE,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
  );

-- Create index on email for faster lookups
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);

-- Insert sample data
INSERT INTO users (name, email) VALUES
                                  ('John Doe', 'john.doe@example.com'),
                                  ('Jane Smith', 'jane.smith@example.com'),
                                  ('Bob Johnson', 'bob.johnson@example.com'),
                                  ('Alice Brown', 'alice.brown@example.com'),
                                  ('Charlie Wilson', 'charlie.wilson@example.com')
  ON CONFLICT (email) DO NOTHING;

-- Create a function to update the updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
RETURN NEW;
END;
$$ language 'plpgsql';

-- Create trigger to automatically update updated_at
CREATE TRIGGER update_users_updated_at
  BEFORE UPDATE ON users
  FOR EACH ROW
  EXECUTE FUNCTION update_updated_at_column();
