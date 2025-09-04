export interface Book {
  id?: number;
  title: string;
  author: string;
  isbn: string;
  publishedYear: number;
  genre: string;
  description?: string;
}

export interface BookResponse {
  books: Book[];
  total: number;
}
