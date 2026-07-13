-- Create the bucket "attachments" if it doesn't exist
INSERT INTO storage.buckets (id, name, public)
VALUES ('attachments', 'attachments', true)
ON CONFLICT (id) DO NOTHING;

-- Policy to allow anonymous/public select (read) access to the attachments bucket
CREATE POLICY "Allow public read access on attachments"
ON storage.objects FOR SELECT
USING (bucket_id = 'attachments');

-- Policy to allow anonymous/public insert (upload) access to the attachments bucket
CREATE POLICY "Allow public insert access on attachments"
ON storage.objects FOR INSERT
WITH CHECK (bucket_id = 'attachments');

-- Policy to allow anonymous/public update access to the attachments bucket
CREATE POLICY "Allow public update access on attachments"
ON storage.objects FOR UPDATE
USING (bucket_id = 'attachments');

-- Policy to allow anonymous/public delete access to the attachments bucket
CREATE POLICY "Allow public delete access on attachments"
ON storage.objects FOR DELETE
USING (bucket_id = 'attachments');
