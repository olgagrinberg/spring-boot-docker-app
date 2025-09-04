-- Create users table
CREATE TABLE IF NOT EXISTS users (
  id SERIAL PRIMARY KEY,
  name VARCHAR(255) NOT NULL,
  email VARCHAR(255) NOT NULL UNIQUE,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
  );

-- Create books table
CREATE TABLE IF NOT EXISTS books (
  id SERIAL PRIMARY KEY,
  title VARCHAR(500) NOT NULL,
  author VARCHAR(255) NOT NULL,
  isbn VARCHAR(17) NOT NULL UNIQUE,
  published_year INTEGER NOT NULL,
  genre VARCHAR(100) NOT NULL,
  description TEXT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
  );

-- Create indexes
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_books_isbn ON books(isbn);
CREATE INDEX IF NOT EXISTS idx_books_author ON books(author);
CREATE INDEX IF NOT EXISTS idx_books_genre ON books(genre);
CREATE INDEX IF NOT EXISTS idx_books_title ON books(title);

-- Insert sample users
INSERT INTO users (name, email) VALUES
                                  ('John Doe', 'john.doe@example.com'),
                                  ('Jane Smith', 'jane.smith@example.com'),
                                  ('Bob Johnson', 'bob.johnson@example.com'),
                                  ('Alice Brown', 'alice.brown@example.com'),
                                  ('Charlie Wilson', 'charlie.wilson@example.com')
  ON CONFLICT (email) DO NOTHING;

-- Insert sample books
INSERT INTO books (title, author, isbn, published_year, genre, description) VALUES
                                                                              ('The Great Gatsby', 'F. Scott Fitzgerald', '978-0-7432-7356-5', 1925, 'Fiction', 'A classic American novel set in the Jazz Age.'),
                                                                              ('To Kill a Mockingbird', 'Harper Lee', '978-0-06-112008-4', 1960, 'Fiction', 'A gripping tale of racial injustice and childhood innocence.'),
                                                                              ('1984', 'George Orwell', '978-0-452-28423-4', 1949, 'Science Fiction', 'A dystopian social science fiction novel.'),
                                                                              ('Pride and Prejudice', 'Jane Austen', '978-0-14-143951-8', 1813, 'Romance', 'A romantic novel of manners written by Jane Austen.'),
                                                                              ('The Catcher in the Rye', 'J.D. Salinger', '978-0-316-76948-0', 1951, 'Fiction', 'A controversial novel about teenage rebellion.'),
                                                                              ('Lord of the Flies', 'William Golding', '978-0-571-05686-2', 1954, 'Fiction', 'A novel about British boys stranded on an uninhabited island.'),
                                                                              ('The Hobbit', 'J.R.R. Tolkien', '978-0-547-92822-7', 1937, 'Fantasy', 'A fantasy novel about the adventures of Bilbo Baggins.'),
                                                                              ('Fahrenheit 451', 'Ray Bradbury', '978-1-4516-7331-9', 1953, 'Science Fiction', 'A dystopian novel about a future where books are banned.'),
                                                                              ('Jane Eyre', 'Charlotte Brontë', '978-0-14-144114-6', 1847, 'Romance', 'A bildungsroman following the experiences of its eponymous heroine.'),
                                                                              ('The Art of War', 'Sun Tzu', '978-0-14-044915-7', -500, 'Non-Fiction', 'An ancient Chinese military treatise.')
  ON CONFLICT (isbn) DO NOTHING;

-- Create functions to update the updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
RETURN NEW;
END;
$ language 'plpgsql';

-- Create triggers to automatically update updated_at
CREATE TRIGGER update_users_updated_at
  BEFORE UPDATE ON users
  FOR EACH ROW
  EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_books_updated_at
  BEFORE UPDATE ON books
  FOR EACH ROW
  EXECUTE FUNCTION update_updated_at_column();-- Create users table
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
